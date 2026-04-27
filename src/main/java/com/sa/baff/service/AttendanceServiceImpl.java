package com.sa.baff.service;

import com.sa.baff.domain.*;
import com.sa.baff.domain.type.AccountLinkStatus;
import com.sa.baff.domain.type.AttendanceSource;
import com.sa.baff.model.dto.AttendanceDto;
import com.sa.baff.model.dto.RewardDto;
import com.sa.baff.repository.*;
import com.sa.baff.service.account.AccountLinkedUserResolver;
import com.sa.baff.util.AdWatchLocation;
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
    private final AdWatchEventRepository adWatchEventRepository;
    private final MissionService missionService;
    private final RewardService rewardService;

    /** 병합 후 streak 산정 대상 source (spec §6.3) */
    private static final Set<AttendanceSource> POST_MERGE_STREAK_SOURCES =
            Set.of(AttendanceSource.TOSS, AttendanceSource.MERGED_TOSS);

    private final Random random = new Random();

    /** 스트릭 보너스 기준 (일수 → 보너스g). RewardConfig에 없으면 기본값 사용 */
    private static final int[][] DEFAULT_STREAK_BONUSES = {
            {3, 2}, {7, 5}, {14, 10}, {30, 20}
    };

    @Override
    public AttendanceDto.checkResponse checkAttendance(String socialId) {
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

        // 연속 출석 보너스 확인
        int streakBonusGrams = 0;
        boolean streakBonusEarned = false;

        List<RewardConfig> streakConfigs =
                rewardConfigRepository.findActiveConfigs(RewardType.ATTENDANCE_STREAK);

        if (!streakConfigs.isEmpty()) {
            for (RewardConfig config : streakConfigs) {
                if (config.getThreshold() != null && config.getThreshold() == newStreakCount) {
                    streakBonusGrams = config.getAmount();
                    streakBonusEarned = true;
                    break;
                }
            }
        } else {
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

        // 첫 출석 프로모션 (토스포인트 직접 지급, 1회) — 내부 dedup, 예외 swallow
        rewardService.claimFirstAttendanceBonus(userId, socialId);

        log.info("출석 완료: userId={}, streak={}, earned={}g, bonus={}g",
                userId, newStreakCount, earnedGrams, streakBonusGrams);

        return AttendanceDto.checkResponse.builder()
                .earnedGrams(earnedGrams)
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

        Optional<UserAttendance> todayAnySource =
                userAttendanceRepository.findByUserIdAndAttendanceDate(userId, today);
        boolean attendedToday = todayAnySource.isPresent();

        Optional<UserAttendance> todayRecord = streakSources == null
                ? todayAnySource
                : userAttendanceRepository.findByUserIdAndAttendanceDateAndSourceIn(userId, today, streakSources);

        int currentStreak;
        if (todayRecord.isPresent()) {
            currentStreak = todayRecord.get().getStreakCount();
        } else {
            Optional<UserAttendance> yesterdayRecord = streakSources == null
                    ? userAttendanceRepository.findByUserIdAndAttendanceDate(userId, today.minusDays(1))
                    : userAttendanceRepository.findByUserIdAndAttendanceDateAndSourceIn(userId, today.minusDays(1), streakSources);
            currentStreak = yesterdayRecord.map(UserAttendance::getStreakCount).orElse(0);
        }

        // canSaveStreak: 오늘 미출석 + 어제 미출석 + 그저께 출석 (S7-14)
        boolean canSaveStreak = computeCanSaveStreak(userId, today, attendedToday, streakSources);

        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        List<LocalDate> monthAttendance = streakSources == null
                ? userAttendanceRepository.findAttendanceDatesInMonth(userId, monthStart, monthEnd)
                : userAttendanceRepository.findAttendanceDatesInMonthBySource(userId, monthStart, monthEnd, streakSources);

        List<AttendanceDto.streakBonusInfo> nextBonuses = buildNextBonuses(currentStreak);

        return AttendanceDto.statusResponse.builder()
                .attendedToday(attendedToday)
                .currentStreak(currentStreak)
                .canSaveStreak(canSaveStreak)
                .monthAttendance(monthAttendance)
                .nextBonuses(nextBonuses)
                .build();
    }

    @Override
    public AttendanceDto.adBonusResponse grantAdBonus(String socialId, String platform, String adFormat) {
        // 핵심 리워드 지급은 기존 RewardService 로직 재사용 (중복/한도 체크 포함)
        RewardDto.rewardResponse reward = rewardService.grantAttendanceAdBonus(socialId);

        // AdWatchEvent 기록 — tossAdResponse에 adFormat 저장
        UserB user = findUser(socialId);
        AdWatchEvent event = new AdWatchEvent(
                user.getId(),
                AdWatchLocation.ATTENDANCE_AD_BONUS,
                null,
                adFormat != null ? adFormat : "UNKNOWN");
        adWatchEventRepository.save(event);

        log.info("출석 광고 보너스 (S7-14): userId={}, platform={}, adFormat={}, earned={}g",
                user.getId(), platform, adFormat, reward.getEarnedGrams());

        return AttendanceDto.adBonusResponse.builder()
                .earnedGrams(reward.getEarnedGrams())
                .build();
    }

    @Override
    public AttendanceDto.streakSaveResponse saveStreak(String socialId, String platform, String adFormat) {
        UserB user = findUser(socialId);
        Long userId = user.getId();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate dayBeforeYesterday = today.minusDays(2);
        Collection<AttendanceSource> streakSources = streakSourcesFor(userId);

        // 조건 재검증 (FE는 canSaveStreak=true 기준이지만 서버에서 다시 확인)
        if (userAttendanceRepository.findByUserIdAndAttendanceDate(userId, today).isPresent()) {
            throw new IllegalArgumentException("오늘 이미 출석했어요.");
        }
        Optional<UserAttendance> yesterdayRecord = streakSources == null
                ? userAttendanceRepository.findByUserIdAndAttendanceDate(userId, yesterday)
                : userAttendanceRepository.findByUserIdAndAttendanceDateAndSourceIn(userId, yesterday, streakSources);
        if (yesterdayRecord.isPresent()) {
            throw new IllegalArgumentException("어제 이미 출석한 상태예요.");
        }
        Optional<UserAttendance> dayBeforeRecord = streakSources == null
                ? userAttendanceRepository.findByUserIdAndAttendanceDate(userId, dayBeforeYesterday)
                : userAttendanceRepository.findByUserIdAndAttendanceDateAndSourceIn(userId, dayBeforeYesterday, streakSources);
        if (dayBeforeRecord.isEmpty()) {
            throw new IllegalArgumentException("그저께 출석 기록이 없어 연속을 유지할 수 없어요.");
        }

        int newStreakCount = dayBeforeRecord.get().getStreakCount() + 1;

        UserAttendance virtualAttendance = new UserAttendance(userId, yesterday, newStreakCount, true);
        if (streakSources != null) {
            virtualAttendance.setSource(AttendanceSource.TOSS);
        }
        // flush 시점에 유니크 제약 위반이면 DataIntegrityViolationException → GlobalExceptionHandler 409
        userAttendanceRepository.saveAndFlush(virtualAttendance);

        AdWatchEvent event = new AdWatchEvent(
                userId,
                AdWatchLocation.ATTENDANCE_STREAK_SAVE,
                null,
                adFormat != null ? adFormat : "UNKNOWN");
        adWatchEventRepository.save(event);

        log.info("출석 연속 유지 (S7-14): userId={}, platform={}, adFormat={}, newStreak={}",
                userId, platform, adFormat, newStreakCount);

        return AttendanceDto.streakSaveResponse.builder()
                .streakCount(newStreakCount)
                .earnedGrams(0)
                .build();
    }

    // === private helpers ===

    private boolean computeCanSaveStreak(
            Long userId,
            LocalDate today,
            boolean attendedToday,
            Collection<AttendanceSource> streakSources) {
        if (attendedToday) {
            return false;
        }
        LocalDate yesterday = today.minusDays(1);
        LocalDate dayBeforeYesterday = today.minusDays(2);

        Optional<UserAttendance> yesterdayRecord = streakSources == null
                ? userAttendanceRepository.findByUserIdAndAttendanceDate(userId, yesterday)
                : userAttendanceRepository.findByUserIdAndAttendanceDateAndSourceIn(userId, yesterday, streakSources);
        if (yesterdayRecord.isPresent()) {
            return false;
        }
        Optional<UserAttendance> dayBeforeRecord = streakSources == null
                ? userAttendanceRepository.findByUserIdAndAttendanceDate(userId, dayBeforeYesterday)
                : userAttendanceRepository.findByUserIdAndAttendanceDateAndSourceIn(userId, dayBeforeYesterday, streakSources);
        return dayBeforeRecord.isPresent();
    }

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

        if (configs.size() == 1 && Boolean.TRUE.equals(configs.get(0).getIsFixed())) {
            return configs.get(0).getAmount();
        }

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
