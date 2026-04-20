package com.sa.baff.service;

import com.sa.baff.domain.AiAnalysis;
import com.sa.baff.domain.AiFeatureConfig;
import com.sa.baff.domain.RunningRecord;
import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.AiAnalysisDto;
import com.sa.baff.model.dto.RunningRecordDto;
import com.sa.baff.model.vo.RunningRecordVO;
import com.sa.baff.repository.AiAnalysisRepository;
import com.sa.baff.repository.AiFeatureConfigRepository;
import com.sa.baff.repository.RunningRecordRepository;
import com.sa.baff.service.account.AccountLinkedUserResolver;
import com.sa.baff.util.AiFeatureType;
import com.sa.baff.util.DateTimeUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RunningRecordServiceImpl implements RunningRecordService {

    private final RunningRecordRepository runningRecordRepository;
    private final AccountLinkedUserResolver accountLinkedUserResolver;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final AiFeatureConfigRepository aiFeatureConfigRepository;
    private final AnthropicApiClient anthropicApiClient;

    @Override
    public Long recordRunning(RunningRecordVO.RecordRunning param, String socialId) {
        UserB user = accountLinkedUserResolver.resolveActiveUserBySocialId(socialId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 같은 날짜에도 여러 건 추가 가능
        RunningRecord newRecord = RunningRecord.builder()
                .user(user)
                .recordDate(param.getRecordDate())
                .durationMinutes(param.getDurationMinutes())
                .distanceKm(param.getDistanceKm())
                .build();
        return runningRecordRepository.save(newRecord).getId();
    }

    @Override
    public RunningRecordDto.GetRunningList getRunningList(String socialId) {
        UserB user = accountLinkedUserResolver.resolveActiveUserBySocialId(socialId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<RunningRecord> records = runningRecordRepository
                .findByUserAndDelYnOrderByRecordDateDesc(user, 'N');

        RunningRecordDto.GetRunningList data = new RunningRecordDto.GetRunningList();

        if (records.isEmpty()) {
            data.setRecords(List.of());
            data.setTotalRecords(0);
            data.setTotalDistanceKm(0.0);
            data.setTotalDurationMinutes(0);
            return data;
        }

        List<RunningRecordDto.RunningRecordResponse> responseList = records.stream()
                .map(r -> {
                    RunningRecordDto.RunningRecordResponse res = new RunningRecordDto.RunningRecordResponse();
                    res.setId(r.getId());
                    res.setRecordDate(r.getRecordDate());
                    res.setDurationMinutes(r.getDurationMinutes());
                    res.setDistanceKm(r.getDistanceKm());
                    res.setPaceMinPerKm(r.getDistanceKm() > 0
                            ? (double) r.getDurationMinutes() / r.getDistanceKm() : 0.0);
                    return res;
                })
                .collect(Collectors.toList());

        data.setRecords(responseList);
        data.setTotalRecords(records.size());
        data.setTotalDistanceKm(records.stream().mapToDouble(RunningRecord::getDistanceKm).sum());
        data.setTotalDurationMinutes(records.stream().mapToInt(RunningRecord::getDurationMinutes).sum());

        return data;
    }

    @Override
    public AiAnalysisDto.AnalysisResponse getAnalysis(String socialId) {
        UserB user = accountLinkedUserResolver.resolveActiveUserBySocialId(socialId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // AI 기능 활성화 확인
        boolean aiEnabled = aiFeatureConfigRepository.findByFeatureType(AiFeatureType.RUNNING)
                .map(AiFeatureConfig::getEnabled)
                .orElse(false);

        // 기존 분석 결과 조회
        Optional<AiAnalysis> existingOpt = aiAnalysisRepository
                .findByUserIdAndFeatureType(user.getId(), AiFeatureType.RUNNING);

        // 최신 기록의 modDateTime 확인
        Optional<RunningRecord> latestRecord = runningRecordRepository
                .findFirstByUserAndDelYnOrderByRecordDateDesc(user, 'N');

        if (latestRecord.isEmpty()) {
            AiAnalysisDto.AnalysisResponse response = new AiAnalysisDto.AnalysisResponse();
            response.setFeatureType(AiFeatureType.RUNNING);
            response.setIsStale(false);
            return response;
        }

        LocalDateTime latestRecordTime = latestRecord.get().getModDateTime();

        // 기존 분석이 있고, 최신 기록 이후로 변경이 없으면 캐시 반환
        if (existingOpt.isPresent()) {
            AiAnalysis existing = existingOpt.get();
            boolean isStale = existing.getLatestRecordAt() == null
                    || latestRecordTime.isAfter(existing.getLatestRecordAt());

            if (!isStale || !aiEnabled) {
                return toResponse(existing, isStale);
            }

            // 재분석 필요 + AI 활성화
            return runAnalysis(user, existing, latestRecordTime);
        }

        // 분석 결과 없음
        if (!aiEnabled) {
            AiAnalysisDto.AnalysisResponse response = new AiAnalysisDto.AnalysisResponse();
            response.setFeatureType(AiFeatureType.RUNNING);
            response.setIsStale(true);
            return response;
        }

        // 첫 분석
        return runAnalysis(user, null, latestRecordTime);
    }

    private AiAnalysisDto.AnalysisResponse runAnalysis(UserB user, AiAnalysis existing, LocalDateTime latestRecordTime) {
        List<RunningRecord> records = runningRecordRepository
                .findByUserAndDelYnOrderByRecordDateDesc(user, 'N');

        String systemPrompt = "당신은 달리기 코치 AI입니다. 사용자의 달리기 기록을 분석하고 한국어로 답변해주세요. "
                + "현황 평가, 추세 분석, 개선 제안을 포함해주세요. 마크다운 형식으로 작성하세요.";

        StringBuilder userPrompt = new StringBuilder("아래는 나의 달리기 기록입니다:\n\n");
        for (RunningRecord r : records) {
            double pace = r.getDistanceKm() > 0 ? (double) r.getDurationMinutes() / r.getDistanceKm() : 0;
            userPrompt.append(String.format("- %s: %.1fkm, %d분 (페이스: %.1f분/km)\n",
                    r.getRecordDate().toLocalDate(), r.getDistanceKm(), r.getDurationMinutes(), pace));
        }
        userPrompt.append("\n총 ").append(records.size()).append("회 기록을 분석해주세요.");

        AnthropicApiClient.DualAnalysisResult result = anthropicApiClient.analyzeDual(
                systemPrompt, userPrompt.toString());

        AiAnalysis analysis = existing != null ? existing : AiAnalysis.builder()
                .userId(user.getId())
                .featureType(AiFeatureType.RUNNING)
                .build();

        analysis.setAnalysisHaiku(result.getHaikuResult());
        analysis.setAnalysisSonnet(result.getSonnetResult());
        analysis.setAnalyzedAt(DateTimeUtils.now());
        analysis.setLatestRecordAt(latestRecordTime);

        aiAnalysisRepository.save(analysis);

        return toResponse(analysis, false);
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
