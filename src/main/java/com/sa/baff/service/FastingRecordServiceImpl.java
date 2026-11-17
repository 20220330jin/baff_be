package com.sa.baff.service;

import com.sa.baff.domain.AiAnalysis;
import com.sa.baff.domain.AiFeatureConfig;
import com.sa.baff.domain.FastingRecord;
import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.AiAnalysisDto;
import com.sa.baff.model.dto.FastingRecordDto;
import com.sa.baff.model.vo.FastingRecordVO;
import com.sa.baff.repository.AiAnalysisRepository;
import com.sa.baff.repository.AiFeatureConfigRepository;
import com.sa.baff.repository.FastingRecordRepository;
import com.sa.baff.service.account.AccountLinkedUserResolver;
import com.sa.baff.util.AiFeatureType;
import com.sa.baff.util.DateTimeUtils;
import com.sa.baff.util.FastingEndStatus;
import com.sa.baff.util.FastingMode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FastingRecordServiceImpl implements FastingRecordService {

    private final FastingRecordRepository fastingRecordRepository;
    private final AccountLinkedUserResolver accountLinkedUserResolver;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final AiFeatureConfigRepository aiFeatureConfigRepository;
    private final AnthropicApiClient anthropicApiClient;
    private final RewardService rewardService;

    /** 모드별 기본 목표 시간 */
    private static final Map<FastingMode, Integer> MODE_TARGET_HOURS = Map.of(
            FastingMode.SIXTEEN_EIGHT, 16,
            FastingMode.EIGHTEEN_SIX, 18,
            FastingMode.TWENTY_FOUR, 20,
            FastingMode.FIVE_TWO, 24
    );

    @Override
    public FastingRecordDto.StartFastingResponse startFasting(FastingRecordVO.StartFasting param, String socialId) {
        UserB user = accountLinkedUserResolver.resolveActiveUserBySocialId(socialId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 이미 진행중인 단식이 있는지 확인
        Optional<FastingRecord> active = fastingRecordRepository.findActive(user);
        if (active.isPresent()) {
            throw new IllegalStateException("이미 진행중인 단식이 있습니다.");
        }

        if (param.getMode() == FastingMode.CUSTOM) {
            FastingEndPolicy.validateCustomHours(param.getTargetHours());
        }
        Integer targetHours = param.getMode() == FastingMode.CUSTOM
                ? param.getTargetHours()
                : MODE_TARGET_HOURS.getOrDefault(param.getMode(), 16);

        FastingRecord record = FastingRecord.builder()
                .user(user)
                .startTime(DateTimeUtils.now())
                .mode(param.getMode())
                .targetHours(targetHours)
                .completed(false)
                .build();

        FastingRecord saved = fastingRecordRepository.save(record);
        return new FastingRecordDto.StartFastingResponse(saved.getId());
    }

    /**
     * 단식 종료 (M-043). 세 경로:
     * - unknown=true: 「기억 안 나요」 — 종료 시각 미확인(UNKNOWN)으로 남기고 통계 · 보상 제외
     * - endTime 있음: 식사 시각으로 복구 — 시작+1분 ~ 시작+48시간 · 지금 이하 · 다른 기록과 겹침 금지
     * - endTime 없음: 지금 시각으로 종료 — 시작 후 48시간이 지났으면 거절(식사 시각 필요)
     * 기록은 잠금 조회로 읽어 같은 기록의 동시 요청을 한 번만 처리한다.
     */
    @Override
    public FastingRecordDto.EndFastingResponse endFasting(FastingRecordVO.EndFasting param, String socialId) {
        UserB user = accountLinkedUserResolver.resolveActiveUserBySocialId(socialId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        FastingRecord record = fastingRecordRepository.findByIdForUpdate(param.getFastingRecordId())
                .orElseThrow(() -> new IllegalArgumentException("단식 기록을 찾을 수 없습니다."));

        if (!record.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("본인의 단식 기록만 종료할 수 있습니다.");
        }
        if (record.getEndTime() != null || record.getEndStatus() != null) {
            throw new IllegalStateException("이미 종료된 단식이에요.");
        }

        if (Boolean.TRUE.equals(param.getUnknown())) {
            record.setEndStatus(FastingEndStatus.UNKNOWN);
            fastingRecordRepository.save(record);
            return new FastingRecordDto.EndFastingResponse(record.getId(), null, false, 0, FastingEndStatus.UNKNOWN);
        }

        LocalDateTime now = DateTimeUtils.now();
        LocalDateTime endTime = resolveEndTime(record, user, param.getEndTime(), now);
        long actualMinutes = FastingEndPolicy.elapsedMinutes(record.getStartTime(), endTime);
        boolean completed = actualMinutes >= (record.getTargetHours() * 60L);

        record.setEndTime(endTime);
        record.setActualMinutes((int) actualMinutes);
        record.setCompleted(completed);
        record.setEndStatus(FastingEndStatus.CONFIRMED);
        fastingRecordRepository.save(record);

        // 목표 달성 시 그램 자동 지급 — 지급 못 하면 0 (예외 없음 · 같은 트랜잭션이라 예외면 종료까지 취소된다)
        int earnedGrams = completed ? rewardService.tryGrantFastingCompleteReward(socialId, record.getId()) : 0;

        return new FastingRecordDto.EndFastingResponse(
                record.getId(), (int) actualMinutes, completed, earnedGrams, FastingEndStatus.CONFIRMED);
    }

    /** 종료 시각 결정 — 입력이 있으면 검사 후 그 시각, 없으면 지금(상한 안일 때만) */
    private LocalDateTime resolveEndTime(FastingRecord record, UserB user, LocalDateTime requested, LocalDateTime now) {
        if (requested == null) {
            if (FastingEndPolicy.needsEndTime(record.getStartTime(), now)) {
                throw new IllegalStateException("식사 시각을 입력해 주세요.");
            }
            return now;
        }
        LocalDateTime endTime = requested.withSecond(0).withNano(0);
        FastingEndPolicy.validateEndTime(record.getStartTime(), endTime, now);
        if (fastingRecordRepository.existsOverlap(user, record.getId(), record.getStartTime(), endTime)) {
            throw new IllegalArgumentException("다른 단식 기록과 시간이 겹쳐요.");
        }
        return endTime;
    }

    @Override
    public FastingRecordDto.ActiveFasting getActiveFasting(String socialId) {
        UserB user = accountLinkedUserResolver.resolveActiveUserBySocialId(socialId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Optional<FastingRecord> active = fastingRecordRepository.findActive(user);

        if (active.isEmpty()) {
            return null;
        }

        FastingRecord record = active.get();
        long elapsed = Duration.between(record.getStartTime(), DateTimeUtils.now()).toMinutes();

        FastingRecordDto.ActiveFasting dto = new FastingRecordDto.ActiveFasting();
        dto.setId(record.getId());
        dto.setStartTime(record.getStartTime());
        dto.setMode(record.getMode());
        dto.setTargetHours(record.getTargetHours());
        dto.setElapsedMinutes(elapsed);
        dto.setNeedsEndTime(FastingEndPolicy.needsEndTime(record.getStartTime(), DateTimeUtils.now()));
        dto.setMaxHours(FastingEndPolicy.MAX_FASTING_HOURS);

        return dto;
    }

    @Override
    public FastingRecordDto.GetFastingList getFastingList(String socialId) {
        UserB user = accountLinkedUserResolver.resolveActiveUserBySocialId(socialId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<FastingRecord> records = fastingRecordRepository
                .findByUserAndDelYnOrderByStartTimeDesc(user, 'N');

        FastingRecordDto.GetFastingList data = new FastingRecordDto.GetFastingList();

        if (records.isEmpty()) {
            data.setRecords(List.of());
            data.setTotalRecords(0);
            data.setCompletedRecords(0);
            data.setTotalFastingMinutes(0);
            data.setAverageFastingMinutes(0.0);
            data.setCurrentStreak(0);
            return data;
        }

        // 통계는 확인된 종료 기록만 — 종료 시각 확인 필요(NEEDS_CHECK) · 미확인(UNKNOWN)은 뺀다 (M-043)
        List<FastingRecord> completed = records.stream()
                .filter(FastingEndPolicy::countsInStats)
                .collect(Collectors.toList());
        long activeCount = records.stream()
                .filter(r -> r.getEndTime() == null && r.getEndStatus() == null)
                .count();

        List<FastingRecordDto.FastingRecordResponse> responseList = records.stream()
                .map(r -> {
                    FastingRecordDto.FastingRecordResponse res = new FastingRecordDto.FastingRecordResponse();
                    res.setId(r.getId());
                    res.setStartTime(r.getStartTime());
                    res.setEndTime(r.getEndTime());
                    res.setMode(r.getMode());
                    res.setTargetHours(r.getTargetHours());
                    res.setActualMinutes(r.getActualMinutes());
                    res.setCompleted(r.getCompleted());
                    res.setEndStatus(FastingEndPolicy.effectiveStatus(r));
                    return res;
                })
                .collect(Collectors.toList());

        int totalMinutes = completed.stream()
                .filter(r -> r.getActualMinutes() != null)
                .mapToInt(FastingRecord::getActualMinutes).sum();

        // 연속 달성 일수 계산
        int streak = 0;
        for (FastingRecord r : completed) {
            if (Boolean.TRUE.equals(r.getCompleted())) {
                streak++;
            } else {
                break;
            }
        }

        data.setRecords(responseList);
        data.setTotalRecords((int) (activeCount + completed.size()));
        data.setCompletedRecords((int) completed.stream().filter(r -> Boolean.TRUE.equals(r.getCompleted())).count());
        data.setTotalFastingMinutes(totalMinutes);
        data.setAverageFastingMinutes(completed.isEmpty() ? 0.0 : (double) totalMinutes / completed.size());
        data.setCurrentStreak(streak);

        return data;
    }

    @Override
    public AiAnalysisDto.AnalysisResponse getAnalysis(String socialId) {
        UserB user = accountLinkedUserResolver.resolveActiveUserBySocialId(socialId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean aiEnabled = aiFeatureConfigRepository.findByFeatureType(AiFeatureType.FASTING)
                .map(AiFeatureConfig::getEnabled)
                .orElse(false);

        Optional<AiAnalysis> existingOpt = aiAnalysisRepository
                .findByUserIdAndFeatureType(user.getId(), AiFeatureType.FASTING);

        Optional<FastingRecord> latestRecord = fastingRecordRepository
                .findFirstByUserAndDelYnOrderByStartTimeDesc(user, 'N');

        if (latestRecord.isEmpty()) {
            AiAnalysisDto.AnalysisResponse response = new AiAnalysisDto.AnalysisResponse();
            response.setFeatureType(AiFeatureType.FASTING);
            response.setIsStale(false);
            return response;
        }

        LocalDateTime latestRecordTime = latestRecord.get().getModDateTime();

        if (existingOpt.isPresent()) {
            AiAnalysis existing = existingOpt.get();
            boolean isStale = existing.getLatestRecordAt() == null
                    || latestRecordTime.isAfter(existing.getLatestRecordAt());

            if (!isStale || !aiEnabled) {
                return toResponse(existing, isStale);
            }
            return runAnalysis(user, existing, latestRecordTime);
        }

        if (!aiEnabled) {
            AiAnalysisDto.AnalysisResponse response = new AiAnalysisDto.AnalysisResponse();
            response.setFeatureType(AiFeatureType.FASTING);
            response.setIsStale(true);
            return response;
        }

        return runAnalysis(user, null, latestRecordTime);
    }

    private AiAnalysisDto.AnalysisResponse runAnalysis(UserB user, AiAnalysis existing, LocalDateTime latestRecordTime) {
        List<FastingRecord> records = fastingRecordRepository
                .findByUserAndDelYnOrderByStartTimeDesc(user, 'N')
                .stream()
                .filter(FastingEndPolicy::countsInStats) // M-043: 확인 필요 · 미확인 기록은 분석에서 뺀다
                .collect(Collectors.toList());

        String systemPrompt = "당신은 간헐적 단식 전문 코치 AI입니다. 사용자의 단식 기록을 분석하고 한국어로 답변해주세요. "
                + "달성률, 패턴 분석, 모드 추천, 개선 제안을 포함해주세요. 마크다운 형식으로 작성하세요.";

        StringBuilder userPrompt = new StringBuilder("아래는 나의 간헐적 단식 기록입니다:\n\n");
        for (FastingRecord r : records) {
            String modeLabel = getModeLabel(r.getMode());
            userPrompt.append(String.format("- %s: %s모드, 목표 %d시간, 실제 %d분(%s)\n",
                    r.getStartTime().toLocalDate(), modeLabel, r.getTargetHours(),
                    r.getActualMinutes() != null ? r.getActualMinutes() : 0,
                    Boolean.TRUE.equals(r.getCompleted()) ? "달성" : "미달성"));
        }
        userPrompt.append("\n총 ").append(records.size()).append("회 기록을 분석해주세요.");

        AnthropicApiClient.DualAnalysisResult result = anthropicApiClient.analyzeDual(
                systemPrompt, userPrompt.toString());

        AiAnalysis analysis = existing != null ? existing : AiAnalysis.builder()
                .userId(user.getId())
                .featureType(AiFeatureType.FASTING)
                .build();

        analysis.setAnalysisHaiku(result.getHaikuResult());
        analysis.setAnalysisSonnet(result.getSonnetResult());
        analysis.setAnalyzedAt(DateTimeUtils.now());
        analysis.setLatestRecordAt(latestRecordTime);

        aiAnalysisRepository.save(analysis);

        return toResponse(analysis, false);
    }

    private String getModeLabel(FastingMode mode) {
        return switch (mode) {
            case SIXTEEN_EIGHT -> "16:8";
            case EIGHTEEN_SIX -> "18:6";
            case TWENTY_FOUR -> "20:4";
            case FIVE_TWO -> "5:2";
            case CUSTOM -> "커스텀";
        };
    }

    private AiAnalysisDto.AnalysisResponse toResponse(AiAnalysis analysis, boolean isStale) {
        AiAnalysisDto.AnalysisResponse response = new AiAnalysisDto.AnalysisResponse();
        response.setFeatureType(analysis.getFeatureType());
        response.setAnalysisHaiku(analysis.getAnalysisHaiku());
        response.setAnalysisSonnet(analysis.getAnalysisSonnet());
        response.setAnalyzedAt(analysis.getAnalyzedAt());
        response.setIsStale(isStale);
        return response;
    }
}
