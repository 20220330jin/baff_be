package com.sa.baff.service;

import com.sa.baff.domain.*;
import com.sa.baff.domain.type.AccountLinkStatus;
import com.sa.baff.domain.type.AttendanceSource;
import com.sa.baff.model.dto.AttendanceDto;
import com.sa.baff.repository.*;
import com.sa.baff.service.account.AccountLinkedUserResolver;
import com.sa.baff.util.MissionType;
import com.sa.baff.util.PieceTransactionType;
import com.sa.baff.util.RewardStatus;
import com.sa.baff.util.RewardType;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AccountLinkedUserResolver accountLinkedUserResolver;
    private final AccountLinkRepository accountLinkRepository;
    private final PieceRepository pieceRepository;
    private final PieceTransactionRepository pieceTransactionRepository;
    private final UserAttendanceRepository userAttendanceRepository;
    private final RewardConfigRepository rewardConfigRepository;
    private final RewardHistoryRepository rewardHistoryRepository;
    private final MissionService missionService;

    /** 병합 후 streak 산정 대상 source (spec §6.3) */
    private static final Set<AttendanceSource> POST_MERGE_STREAK_SOURCES =
            Set.of(AttendanceSource.TOSS, AttendanceSource.MERGED_TOSS);

    private final Random random = new Random();

    /** 스트릭 보너스 기준 (일수 → 보너스g). RewardConfig에 없으면 기본값 사용 */
    private static final int[][] DEFAULT_STREAK_BONUSES = {
            {3, 2}, {7, 5}, {14, 10}, {30, 20}
    };

    @Override
    public AttendanceDto.checkResponse checkAttendance(String socialId, Boolean preAdWatched) {
        UserB user = findUser(socialId);
        Long userId = user.getId();
        LocalDate today = LocalDate.now();

        Collection<AttendanceSource> streakSources = streakSourcesFor(userId);

        // 중복 출석 체크 (DB unique 제약은 전체 source 대상이므로 필터 없이 확인)
        if (userAttendanceRepository.findByUserIdAndAttendanceDate(userId, today).isPresent()) {
            throw new IllegalStateException("오늘은 이미 출석했어요.");
        }

        // 스트릭 계산 (어제 출석 여부 — 병합 후 source 필터)
        LocalDate yesterday = today.minusDays(1);
        Optional<UserAttendance> yesterdayAttendance = streakSources == null
                ? userAttendanceRepository.findByUserIdAndAttendanceDate(userId, yesterday)
                : userAttendanceRepository.findByUserIdAndAttendanceDateAndSourceIn(userId, yesterday, streakSources);
        int newStreakCount = yesterdayAttendance.isPresent()
                ? yesterdayAttendance.get().getStreakCount() + 1
                : 1;

        // 기본 출석 리워드 지급
        int earnedGrams = determineAmount(RewardType.ATTENDANCE);
        addPointsToUser(user, earnedGrams, PieceTransactionType.REWARD_ATTENDANCE, null);

        RewardHistory history = new RewardHistory(
                userId, RewardType.ATTENDANCE, earnedGrams, RewardStatus.SUCCESS, null);
        rewardHistoryRepository.save(history);

        // 출석 전 광고 보너스
        int preAdBonusGrams = 0;
        if (Boolean.TRUE.equals(preAdWatched)) {
            preAdBonusGrams = determineAmount(RewardType.ATTENDANCE_AD_BONUS);
            if (preAdBonusGrams > 0) {
                addPointsToUser(user, preAdBonusGrams, PieceTransactionType.REWARD_AD_BONUS, null);
                RewardHistory preAdHistory = new RewardHistory(
                        userId, RewardType.ATTENDANCE_AD_BONUS, preAdBonusGrams, RewardStatus.SUCCESS, null);
                rewardHistoryRepository.save(preAdHistory);
            }
        }

        // 연속 출석 보너스 확인
        int streakBonusGrams = 0;
        boolean streakBonusEarned = false;

        List<RewardConfig> streakConfigs =
                rewardConfigRepository.findActiveConfigs(RewardType.ATTENDANCE_STREAK);

        if (!streakConfigs.isEmpty()) {
            // RewardConfig에서 threshold 매칭
            for (RewardConfig config : streakConfigs) {
                if (config.getThreshold() != null && config.getThreshold() == newStreakCount) {
                    streakBonusGrams = config.getAmount();
                    streakBonusEarned = true;
                    break;
                }
            }
        } else {
            // RewardConfig 없으면 기본값 사용
            for (int[] bonus : DEFAULT_STREAK_BONUSES) {
                if (bonus[0] == newStreakCount) {
                    streakBonusGrams = bonus[1];
                    streakBonusEarned = true;
                    break;
                }
            }
        }

        if (streakBonusEarned) {
            addPointsToUser(user, streakBonusGrams, PieceTransactionType.REWARD_STREAK_ATTENDANCE, null);

            RewardHistory bonusHistory = new RewardHistory(
                    userId, RewardType.ATTENDANCE_STREAK, streakBonusGrams, RewardStatus.SUCCESS, null);
            rewardHistoryRepository.save(bonusHistory);
        }

        // 출석 기록 저장 (병합된 사용자의 신규 출석은 TOSS source)
        UserAttendance attendance = new UserAttendance(userId, today, newStreakCount, false);
        if (streakSources != null) {
            attendance.setSource(AttendanceSource.TOSS);
        }
        userAttendanceRepository.save(attendance);

        // 이번주 미션 진행도 증가
        missionService.incrementMissionProgress(userId, MissionType.WEEKLY_ATTENDANCE);

        log.info("출석 완료: userId={}, streak={}, earned={}g, preAdBonus={}g, bonus={}g",
                userId, newStreakCount, earnedGrams, preAdBonusGrams, streakBonusGrams);

        return AttendanceDto.checkResponse.builder()
                .earnedGrams(earnedGrams)
                .preAdBonusGrams(preAdBonusGrams)
                .streakCount(newStreakCount)
                .streakBonusEarned(streakBonusEarned)
                .streakBonusGrams(streakBonusGrams)
                .build();
    }

    @Override
    public AttendanceDto.statusResponse getStatus(String socialId) {
        UserB user = findUser(socialId);
        Long userId = user.getId();
        LocalDate today = LocalDate.now();
        Collection<AttendanceSource> streakSources = streakSourcesFor(userId);

        // S3-15 P1-4: attendedToday는 전체 source 기준으로 판정.
        // checkAttendance의 DB unique 제약이 (user_id, attendance_date)라 source 무관 중복 불가.
        // 병합 사용자가 오늘 WEB에서만 출석한 경우 FE가 attendedToday=false로 보고 버튼 노출 → API가 "이미 출석" 에러를 반환하는 UX 불일치를 제거.
        Optional<UserAttendance> todayAnySource =
                userAttendanceRepository.findByUserIdAndAttendanceDate(userId, today);
        boolean attendedToday = todayAnySource.isPresent();

        // streak 산정용 오늘 row는 여전히 source 필터 (spec §6.3 — 병합 후 WEB은 streak 제외)
        Optional<UserAttendance> todayRecord = streakSources == null
                ? todayAnySource
                : userAttendanceRepository.findByUserIdAndAttendanceDateAndSourceIn(userId, today, streakSources);

        // 현재 스트릭 — streak 포함 source(todayRecord)가 있으면 그 값, 아니면 어제 기반.
        // P1-4: 병합 사용자가 오늘 WEB에서만 출석한 경우 attendedToday=true지만 todayRecord empty → 어제 streak 유지.
        int currentStreak;
        if (todayRecord.isPresent()) {
            currentStreak = todayRecord.get().getStreakCount();
        } else {
            Optional<UserAttendance> yesterdayRecord = streakSources == null
                    ? userAttendanceRepository.findByUserIdAndAttendanceDate(userId, today.minusDays(1))
                    : userAttendanceRepository.findByUserIdAndAttendanceDateAndSourceIn(userId, today.minusDays(1), streakSources);
            currentStreak = yesterdayRecord.map(UserAttendance::getStreakCount).orElse(0);
        }

        // 이번 달 출석 목록 (병합 후 source 필터)
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        List<LocalDate> monthAttendance = streakSources == null
                ? userAttendanceRepository.findAttendanceDatesInMonth(userId, monthStart, monthEnd)
                : userAttendanceRepository.findAttendanceDatesInMonthBySource(userId, monthStart, monthEnd, streakSources);

        // 다음 보너스 목록
        List<AttendanceDto.streakBonusInfo> nextBonuses = buildNextBonuses(currentStreak);

        return AttendanceDto.statusResponse.builder()
                .attendedToday(attendedToday)
                .currentStreak(currentStreak)
                .monthAttendance(monthAttendance)
                .nextBonuses(nextBonuses)
                .build();
    }

    // === private helpers ===

    private UserB findUser(String socialId) {
        return accountLinkedUserResolver.resolveActiveUserBySocialId(socialId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
    }

    /**
     * 병합된 사용자의 streak 산정 대상 source를 반환. 병합되지 않은 사용자는 null(기존 전체 집계).
     * spec §6.3: 병합 후 웹 출석(WEB)은 streak에서 제외.
     */
    private Collection<AttendanceSource> streakSourcesFor(Long userId) {
        boolean isMerged = accountLinkRepository
                .existsByUserIdAndStatus(userId, AccountLinkStatus.ACTIVE);
        return isMerged ? POST_MERGE_STREAK_SOURCES : null;
    }

    private void addPointsToUser(UserB user, int amount, PieceTransactionType txType, Long referenceId) {
        Piece piece = pieceRepository.findByUser(user)
                .orElseGet(() -> pieceRepository.save(new Piece(user)));

        piece.addReward((long) amount);
        pieceRepository.save(piece);

        PieceTransaction tx = PieceTransaction.builder()
                .user(user)
                .amount((long) amount)
                .type(txType)
                .referenceId(referenceId)
                .build();
        pieceTransactionRepository.save(tx);
    }

    /** 확률 기반 금액 결정 (나만그래 패턴) */
    private int determineAmount(RewardType rewardType) {
        List<RewardConfig> configs = rewardConfigRepository.findActiveConfigs(rewardType);

        if (configs.isEmpty()) {
            return 1; // 설정 없으면 기본 1g
        }

        // 고정 금액이면 바로 반환
        if (configs.size() == 1 && Boolean.TRUE.equals(configs.get(0).getIsFixed())) {
            return configs.get(0).getAmount();
        }

        // 확률 기반 결정
        int rand = random.nextInt(100);
        int cumulative = 0;
        for (RewardConfig config : configs) {
            cumulative += config.getProbability();
            if (rand < cumulative) {
                return config.getAmount();
            }
        }
        return configs.get(configs.size() - 1).getAmount();
    }

    private List<AttendanceDto.streakBonusInfo> buildNextBonuses(int currentStreak) {
        List<AttendanceDto.streakBonusInfo> bonuses = new ArrayList<>();
        List<RewardConfig> streakConfigs =
                rewardConfigRepository.findActiveConfigs(RewardType.ATTENDANCE_STREAK);

        if (!streakConfigs.isEmpty()) {
            for (RewardConfig config : streakConfigs) {
                if (config.getThreshold() != null) {
                    bonuses.add(AttendanceDto.streakBonusInfo.builder()
                            .requiredDays(config.getThreshold())
                            .bonusGrams(config.getAmount())
                            .achieved(currentStreak >= config.getThreshold())
                            .build());
                }
            }
        } else {
            for (int[] bonus : DEFAULT_STREAK_BONUSES) {
                bonuses.add(AttendanceDto.streakBonusInfo.builder()
                        .requiredDays(bonus[0])
                        .bonusGrams(bonus[1])
                        .achieved(currentStreak >= bonus[0])
                        .build());
            }
        }

        return bonuses;
    }
}
