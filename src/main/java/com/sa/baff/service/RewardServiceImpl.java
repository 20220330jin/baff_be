package com.sa.baff.service;

import com.sa.baff.common.GramConstants;
import com.sa.baff.domain.*;
import com.sa.baff.model.dto.RewardDto;
import com.sa.baff.repository.*;
import com.sa.baff.service.account.AccountLinkedUserResolver;
import com.sa.baff.util.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RewardServiceImpl implements RewardService {

    private final AccountLinkedUserResolver accountLinkedUserResolver;
    private final PieceRepository pieceRepository;
    private final PieceTransactionRepository pieceTransactionRepository;
    private final RewardConfigRepository rewardConfigRepository;
    private final RewardHistoryRepository rewardHistoryRepository;
    private final UserRewardDailyRepository userRewardDailyRepository;
    private final UserRewardWeeklyRepository userRewardWeeklyRepository;
    private final AdWatchEventRepository adWatchEventRepository;
    private final TossPromotionApiClient tossPromotionApiClient;

    private final Random random = new Random();

    @Override
    public RewardDto.rewardResponse grantWeightReward(String socialId, Long weightId) {
        UserB user = findUser(socialId);
        Long userId = user.getId();

        // S6-14: 첫 체중 리워드 호출 시점에 가입 축하 보너스 지급 (1회성, 내부 dedup + enabled 체크).
        // WEIGHT_LOG 일일제한/실패와 독립 — 이미 SUCCESS면 내부 skip.
        claimSignupBonus(userId, user);

        // 일일 제한 체크
        checkDailyLimit(userId, RewardType.WEIGHT_LOG);

        // 금액 결정
        int earnedGrams = determineAmount(RewardType.WEIGHT_LOG);

        // 포인트 적립
        addPointsToUser(user, earnedGrams, PieceTransactionType.REWARD_WEIGHT_LOG, weightId);

        // 리워드 기록
        RewardHistory history = new RewardHistory(
                userId, RewardType.WEIGHT_LOG, earnedGrams, RewardStatus.SUCCESS, weightId);
        rewardHistoryRepository.save(history);

        // 일일 집계 업데이트
        incrementDaily(userId, RewardType.WEIGHT_LOG, earnedGrams);

        // S6-28 주간 마일스톤 체크 (3/5/7회 달성 시 자동 보너스) — 예외는 내부 swallow
        claimWeeklyMilestones(userId, user);

        log.info("체중 기록 리워드: userId={}, weightId={}, earned={}g", userId, weightId, earnedGrams);

        return RewardDto.rewardResponse.builder()
                .earnedGrams(earnedGrams)
                .message(GramConstants.earnMessage(earnedGrams))
                .build();
    }

    @Override
    public RewardDto.rewardResponse grantReviewReward(String socialId, Long reviewId) {
        UserB user = findUser(socialId);
        Long userId = user.getId();

        // 일일 제한 체크
        checkDailyLimit(userId, RewardType.REVIEW);

        // 금액 결정
        int earnedGrams = determineAmount(RewardType.REVIEW);

        // 포인트 적립
        addPointsToUser(user, earnedGrams, PieceTransactionType.REWARD_REVIEW, reviewId);

        // 리워드 기록
        RewardHistory history = new RewardHistory(
                userId, RewardType.REVIEW, earnedGrams, RewardStatus.SUCCESS, reviewId);
        rewardHistoryRepository.save(history);

        // 일일 집계 업데이트
        incrementDaily(userId, RewardType.REVIEW, earnedGrams);

        log.info("리뷰 리워드: userId={}, reviewId={}, earned={}g", userId, reviewId, earnedGrams);

        return RewardDto.rewardResponse.builder()
                .earnedGrams(earnedGrams)
                .message(GramConstants.earnMessage(earnedGrams))
                .build();
    }

    @Override
    public RewardDto.rewardResponse grantWeightAdBonus(String socialId, Long weightId) {
        UserB user = findUser(socialId);
        Long userId = user.getId();

        checkDailyLimit(userId, RewardType.WEIGHT_AD_BONUS);

        int earnedGrams = determineAmount(RewardType.WEIGHT_AD_BONUS);

        addPointsToUser(user, earnedGrams, PieceTransactionType.REWARD_WEIGHT_LOG, weightId);

        RewardHistory history = new RewardHistory(
                userId, RewardType.WEIGHT_AD_BONUS, earnedGrams, RewardStatus.SUCCESS, weightId);
        rewardHistoryRepository.save(history);

        incrementDaily(userId, RewardType.WEIGHT_AD_BONUS, earnedGrams);

        log.info("체중 광고 보너스: userId={}, weightId={}, earned={}g", userId, weightId, earnedGrams);

        return RewardDto.rewardResponse.builder()
                .earnedGrams(earnedGrams)
                .message(GramConstants.earnMessage(earnedGrams))
                .build();
    }

    @Override
    public RewardDto.rewardResponse grantAttendanceAdBonus(String socialId) {
        UserB user = findUser(socialId);
        Long userId = user.getId();

        checkDailyLimit(userId, RewardType.ATTENDANCE_AD_BONUS);

        int earnedGrams = determineAmount(RewardType.ATTENDANCE_AD_BONUS);

        addPointsToUser(user, earnedGrams, PieceTransactionType.REWARD_AD_BONUS, null);

        RewardHistory history = new RewardHistory(
                userId, RewardType.ATTENDANCE_AD_BONUS, earnedGrams, RewardStatus.SUCCESS, null);
        rewardHistoryRepository.save(history);

        incrementDaily(userId, RewardType.ATTENDANCE_AD_BONUS, earnedGrams);

        log.info("출석 광고 보너스: userId={}, earned={}g", userId, earnedGrams);

        return RewardDto.rewardResponse.builder()
                .earnedGrams(earnedGrams)
                .message(GramConstants.earnMessage(earnedGrams))
                .build();
    }

    /**
     * 간헐적 단식 완료 리워드 (목표 달성 시 자동 1회 지급).
     * - RewardConfig.FASTING_COMPLETE 활성화 + dailyLimit 체크
     * - FastingRecordServiceImpl.endFasting에서 호출 (예외 swallow는 호출자에서 처리)
     */
    @Override
    public RewardDto.rewardResponse grantFastingCompleteReward(String socialId, Long fastingRecordId) {
        UserB user = findUser(socialId);
        Long userId = user.getId();

        List<RewardConfig> configs = rewardConfigRepository.findActiveConfigs(RewardType.FASTING_COMPLETE);
        if (configs.isEmpty()) {
            log.info("간헐적 단식 완료 리워드 비활성 (userId={})", userId);
            return RewardDto.rewardResponse.builder().earnedGrams(0).message("").build();
        }

        checkDailyLimit(userId, RewardType.FASTING_COMPLETE);

        int earnedGrams = determineAmount(RewardType.FASTING_COMPLETE);
        addPointsToUser(user, earnedGrams, PieceTransactionType.REWARD_FASTING_COMPLETE, fastingRecordId);

        RewardHistory history = new RewardHistory(
                userId, RewardType.FASTING_COMPLETE, earnedGrams, RewardStatus.SUCCESS, fastingRecordId);
        rewardHistoryRepository.save(history);

        incrementDaily(userId, RewardType.FASTING_COMPLETE, earnedGrams);

        log.info("간헐적 단식 완료 리워드: userId={}, fastingId={}, earned={}g", userId, fastingRecordId, earnedGrams);
        return RewardDto.rewardResponse.builder()
                .earnedGrams(earnedGrams)
                .message(GramConstants.earnMessage(earnedGrams))
                .build();
    }

    /** 간헐적 단식 완료 결과 페이지 광고 보너스 (또받기). */
    @Override
    public RewardDto.rewardResponse grantFastingAdBonus(String socialId) {
        UserB user = findUser(socialId);
        Long userId = user.getId();

        checkDailyLimit(userId, RewardType.FASTING_AD_BONUS);

        int earnedGrams = determineAmount(RewardType.FASTING_AD_BONUS);
        addPointsToUser(user, earnedGrams, PieceTransactionType.REWARD_FASTING_AD_BONUS, null);

        RewardHistory history = new RewardHistory(
                userId, RewardType.FASTING_AD_BONUS, earnedGrams, RewardStatus.SUCCESS, null);
        rewardHistoryRepository.save(history);

        incrementDaily(userId, RewardType.FASTING_AD_BONUS, earnedGrams);

        log.info("간헐적 단식 광고 보너스: userId={}, earned={}g", userId, earnedGrams);
        return RewardDto.rewardResponse.builder()
                .earnedGrams(earnedGrams)
                .message(GramConstants.earnMessage(earnedGrams))
                .build();
    }

    @Override
    public RewardDto.historyResponse getRewardHistory(String socialId) {
        UserB user = findUser(socialId);
        Long userId = user.getId();

        Piece piece = pieceRepository.findByUser(user)
                .orElseGet(() -> pieceRepository.save(new Piece(user)));

        List<RewardHistory> histories = rewardHistoryRepository.findByUserIdOrderByRegDateTimeDesc(userId);

        List<RewardDto.historyItem> items = histories.stream()
                .map(h -> RewardDto.historyItem.builder()
                        .rewardType(h.getRewardType())
                        .amount(h.getAmount())
                        .description(getRewardDescription(h.getRewardType()))
                        .createdAt(h.getRegDateTime())
                        .build())
                .collect(Collectors.toList());

        return RewardDto.historyResponse.builder()
                .balance(piece.getBalance())
                .items(items)
                .build();
    }

    @Override
    public RewardDto.pointBalanceResponse getPointBalance(String socialId) {
        UserB user = findUser(socialId);

        Piece piece = pieceRepository.findByUser(user)
                .orElseGet(() -> pieceRepository.save(new Piece(user)));

        return RewardDto.pointBalanceResponse.builder()
                .balance(piece.getBalance())
                .totalEarned(piece.getTotalEarned())
                .totalExchanged(piece.getTotalExchanged())
                .build();
    }

    @Override
    public void recordAdEvent(String socialId, String watchLocation, Long referenceId, String tossAdResponse) {
        try {
            UserB user = findUser(socialId);
            AdWatchLocation location = AdWatchLocation.valueOf(watchLocation);

            AdWatchEvent event = new AdWatchEvent(user.getId(), location, referenceId, tossAdResponse);
            adWatchEventRepository.save(event);
        } catch (Exception e) {
            log.warn("광고 이벤트 기록 실패 (무시): {}", e.getMessage());
        }
    }

    /**
     * S6-14 가입 축하 보너스 지급 (1회성).
     * - RewardConfig.SIGNUP_BONUS 활성화 필요
     * - 동일 유저에 SUCCESS 이력 있으면 skip
     * - 예외는 swallow + warn log로 호출자 경로 영향 억제 (단, 동일 트랜잭션 flush/commit 시점 예외는 격리 불가 — spec §10 리스크 참조)
     */
    @Override
    public void claimSignupBonus(Long userId, UserB user) {
        try {
            List<RewardConfig> configs = rewardConfigRepository.findActiveConfigs(RewardType.SIGNUP_BONUS);
            if (configs.isEmpty()) {
                log.info("가입 축하 보너스 설정 없음 또는 비활성화 (userId={})", userId);
                return;
            }
            boolean already = rewardHistoryRepository
                    .existsByUserIdAndRewardTypeAndStatusAndDelYn(
                            userId, RewardType.SIGNUP_BONUS, RewardStatus.SUCCESS, 'N');
            if (already) {
                log.info("가입 축하 보너스 이미 지급됨 (userId={})", userId);
                return;
            }

            int amount = determineAmount(RewardType.SIGNUP_BONUS);

            addPointsToUser(user, amount, PieceTransactionType.REWARD_SIGNUP_BONUS, null);

            RewardHistory history = new RewardHistory(
                    userId, RewardType.SIGNUP_BONUS, amount, RewardStatus.SUCCESS, null);
            rewardHistoryRepository.save(history);

            log.info("가입 축하 보너스 지급: userId={}, amount={}g", userId, amount);
        } catch (Exception e) {
            log.warn("가입 축하 보너스 지급 실패 (userId={}): {}", userId, e.getMessage());
        }
    }

    /**
     * 프로필 완성 보너스 지급 (필드별 최초 1회성).
     * S6-15: PROFILE_BONUS (height) / S6-30: PROFILE_BONUS_GENDER, PROFILE_BONUS_BIRTHDATE.
     * - RewardConfig 해당 타입 활성화 필요
     * - 동일 유저 + 동일 RewardType에 SUCCESS 이력 있으면 skip
     * - 예외는 swallow + warn log로 호출자 경로(필드 저장) 영향 억제
     */
    @Override
    public int claimProfileBonus(Long userId, UserB user, RewardType rewardType) {
        try {
            List<RewardConfig> configs = rewardConfigRepository.findActiveConfigs(rewardType);
            if (configs.isEmpty()) {
                log.info("프로필 보너스 설정 없음 또는 비활성화 (userId={}, type={})", userId, rewardType);
                return 0;
            }
            boolean already = rewardHistoryRepository
                    .existsByUserIdAndRewardTypeAndStatusAndDelYn(
                            userId, rewardType, RewardStatus.SUCCESS, 'N');
            if (already) {
                log.info("프로필 보너스 이미 지급됨 (userId={}, type={})", userId, rewardType);
                return 0;
            }

            int amount = determineAmount(rewardType);

            addPointsToUser(user, amount, resolveProfileBonusTxType(rewardType), null);

            RewardHistory history = new RewardHistory(
                    userId, rewardType, amount, RewardStatus.SUCCESS, null);
            rewardHistoryRepository.save(history);

            log.info("프로필 보너스 지급: userId={}, type={}, amount={}g", userId, rewardType, amount);
            return amount;
        } catch (Exception e) {
            log.warn("프로필 보너스 지급 실패 (userId={}, type={}): {}", userId, rewardType, e.getMessage());
            return 0;
        }
    }

    /**
     * 첫 출석 프로모션 보너스 (토스포인트 10원, 생애 1회).
     * 나만그래 FIRST_VOTE_BONUS 패턴 복제.
     * - RewardConfig.FIRST_ATTENDANCE_BONUS enabled=true + promotionCode 비어있지 않을 때만 지급 시도
     * - 이미 SUCCESS/PENDING 이력 있으면 skip (1회성 보장)
     * - 토스 API 호출 실패 시 RewardHistory FAILED로 기록, 예외 swallow (출석 본 경로 영향 없도록)
     * - 토스포인트 직접 지급이므로 그램(piece) 적립 없음
     */
    @Override
    public void claimFirstAttendanceBonus(Long userId, String socialId) {
        try {
            RewardConfig config = rewardConfigRepository
                    .findActiveConfigs(RewardType.FIRST_ATTENDANCE_BONUS).stream()
                    .findFirst().orElse(null);
            if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
                log.debug("첫 출석 프로모션 비활성화 (userId={})", userId);
                return;
            }
            String promotionCode = config.getPromotionCode();
            if (promotionCode == null || promotionCode.isBlank()) {
                log.info("첫 출석 프로모션 코드 미입력 — skip (userId={})", userId);
                return;
            }

            boolean already = rewardHistoryRepository.existsByUserIdAndRewardTypeAndStatusAndDelYn(
                    userId, RewardType.FIRST_ATTENDANCE_BONUS, RewardStatus.SUCCESS, 'N')
                    || rewardHistoryRepository.existsByUserIdAndRewardTypeAndStatusAndDelYn(
                    userId, RewardType.FIRST_ATTENDANCE_BONUS, RewardStatus.PENDING, 'N');
            if (already) {
                log.debug("첫 출석 프로모션 이미 지급됨 또는 진행중 (userId={})", userId);
                return;
            }

            int amount = config.getAmount() != null ? config.getAmount() : GramConstants.FIRST_ATTENDANCE_TOSS_POINT_AMOUNT;

            RewardHistory history = new RewardHistory(
                    userId, RewardType.FIRST_ATTENDANCE_BONUS, amount, RewardStatus.PENDING, null);
            rewardHistoryRepository.save(history);

            try {
                if (!tossPromotionApiClient.isAvailable()) {
                    log.warn("[FIRST_ATTENDANCE_BONUS] tossWebClient 미설정 — 더미 처리 (userId={})", userId);
                    history.setStatus(RewardStatus.SUCCESS);
                    history.setPromotionKey(promotionCode);
                    history.setPromotionTransactionId("dummy");
                } else {
                    String key = tossPromotionApiClient.grantReward(socialId, promotionCode, amount);
                    history.setPromotionKey(promotionCode);
                    history.setPromotionTransactionId(key);
                    history.setStatus(RewardStatus.SUCCESS);
                    log.info("첫 출석 프로모션 토스포인트 {}원 지급 성공 (userId={})", amount, userId);
                }
                rewardHistoryRepository.save(history);
            } catch (Exception e) {
                log.error("첫 출석 프로모션 지급 실패 (userId={}): {}", userId, e.getMessage(), e);
                history.setStatus(RewardStatus.FAILED);
                String msg = e.getMessage();
                history.setErrorMessage(msg != null ? msg.substring(0, Math.min(msg.length(), 500)) : "알 수 없는 오류");
                rewardHistoryRepository.save(history);
            }
        } catch (Exception e) {
            log.warn("첫 출석 프로모션 처리 실패 (userId={}): {}", userId, e.getMessage());
        }
    }

    private PieceTransactionType resolveProfileBonusTxType(RewardType rewardType) {
        return switch (rewardType) {
            case PROFILE_BONUS -> PieceTransactionType.REWARD_PROFILE_BONUS;
            case PROFILE_BONUS_GENDER -> PieceTransactionType.REWARD_PROFILE_BONUS_GENDER;
            case PROFILE_BONUS_BIRTHDATE -> PieceTransactionType.REWARD_PROFILE_BONUS_BIRTHDATE;
            default -> throw new IllegalArgumentException("Not a profile bonus type: " + rewardType);
        };
    }

    /**
     * S6-28 주간 마일스톤 체크 & 지급 (체중기록 3/5/7회).
     * - 이번주 WEIGHT_LOG SUCCESS 카운트 조회 → 3/5/7 도달 시 해당 플래그 미수령이면 지급
     * - dedup: UserRewardWeekly.milestone_N_claimed (user_id + week_start_date unique)
     * - 예외는 swallow + warn log로 호출자 경로(grantWeightReward) 영향 억제
     * - gap_analysis §5-C L1+L2
     */
    @Override
    public void claimWeeklyMilestones(Long userId, UserB user) {
        try {
            LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
            LocalDateTime startOfWeek = weekStart.atStartOfDay();
            LocalDateTime endOfWeek = weekStart.plusDays(7).atStartOfDay();

            long weightLogCount = rewardHistoryRepository
                    .countByUserIdAndRewardTypeAndStatusAndDelYnAndRegDateTimeBetween(
                            userId, RewardType.WEIGHT_LOG, RewardStatus.SUCCESS, 'N',
                            startOfWeek, endOfWeek);

            // 아직 3회 미달이면 early return (DB 접근 최소화)
            if (weightLogCount < 3) return;

            UserRewardWeekly weekly = userRewardWeeklyRepository
                    .findByUserIdAndWeekStartDate(userId, weekStart)
                    .orElseGet(() -> userRewardWeeklyRepository.save(new UserRewardWeekly(userId, weekStart)));

            boolean changed = false;
            if (weightLogCount >= 3 && !weekly.getMilestone3Claimed()) {
                grantMilestone(userId, user, RewardType.WEEKLY_MILESTONE_3,
                        PieceTransactionType.REWARD_WEEKLY_MILESTONE_3);
                weekly.setMilestone3Claimed(true);
                changed = true;
            }
            if (weightLogCount >= 5 && !weekly.getMilestone5Claimed()) {
                grantMilestone(userId, user, RewardType.WEEKLY_MILESTONE_5,
                        PieceTransactionType.REWARD_WEEKLY_MILESTONE_5);
                weekly.setMilestone5Claimed(true);
                changed = true;
            }
            if (weightLogCount >= 7 && !weekly.getMilestone7Claimed()) {
                grantMilestone(userId, user, RewardType.WEEKLY_MILESTONE_7,
                        PieceTransactionType.REWARD_WEEKLY_MILESTONE_7);
                weekly.setMilestone7Claimed(true);
                changed = true;
            }

            if (changed) userRewardWeeklyRepository.save(weekly);
        } catch (Exception e) {
            log.warn("주간 마일스톤 체크/지급 실패 (userId={}): {}", userId, e.getMessage());
        }
    }

    private void grantMilestone(Long userId, UserB user, RewardType rewardType, PieceTransactionType txType) {
        List<RewardConfig> configs = rewardConfigRepository.findActiveConfigs(rewardType);
        if (configs.isEmpty()) {
            log.info("주간 마일스톤 설정 없음 또는 비활성화 (userId={}, type={})", userId, rewardType);
            return;
        }
        int amount = determineAmount(rewardType);
        addPointsToUser(user, amount, txType, null);
        rewardHistoryRepository.save(
                new RewardHistory(userId, rewardType, amount, RewardStatus.SUCCESS, null));
        log.info("주간 마일스톤 지급: userId={}, type={}, amount={}g", userId, rewardType, amount);
    }

    // === private helpers ===

    private UserB findUser(String socialId) {
        return accountLinkedUserResolver.resolveActiveUserBySocialId(socialId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
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

    /** 일일 제한 체크 (초과 시 예외) */
    private void checkDailyLimit(Long userId, RewardType rewardType) {
        List<RewardConfig> configs = rewardConfigRepository.findActiveConfigs(rewardType);

        Integer dailyLimit = configs.stream()
                .map(RewardConfig::getDailyLimit)
                .filter(limit -> limit != null)
                .findFirst()
                .orElse(configs.isEmpty() ? 3 : 1); // config 미설정 시 기본 3회, 설정 시 1회

        UserRewardDaily daily = userRewardDailyRepository
                .findByUserIdAndRewardDateAndRewardType(userId, LocalDate.now(), rewardType)
                .orElse(null);

        if (daily != null && daily.getCount() >= dailyLimit) {
            throw new IllegalStateException("오늘은 이미 리워드를 받았어요.");
        }
    }

    /** 일일 집계 업데이트 */
    private void incrementDaily(Long userId, RewardType rewardType, int amount) {
        LocalDate today = LocalDate.now();
        UserRewardDaily daily = userRewardDailyRepository
                .findByUserIdAndRewardDateAndRewardType(userId, today, rewardType)
                .orElseGet(() -> {
                    UserRewardDaily newDaily = new UserRewardDaily(userId, today, rewardType);
                    return userRewardDailyRepository.save(newDaily);
                });

        daily.increment(amount);
        userRewardDailyRepository.save(daily);
    }

    /** 확률 기반 금액 결정 */
    private int determineAmount(RewardType rewardType) {
        List<RewardConfig> configs = rewardConfigRepository.findActiveConfigs(rewardType);

        if (configs.isEmpty()) {
            return 1;
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

    private String getRewardDescription(RewardType type) {
        return switch (type) {
            case WEIGHT_LOG -> "체중 기록";
            case REVIEW -> "리뷰 작성";
            case ATTENDANCE -> "출석";
            case ATTENDANCE_STREAK -> "연속 출석 보너스";
            case ATTENDANCE_AD_BONUS -> "출석 광고 보너스";
            case WEIGHT_AD_BONUS -> "체중 기록 광고 보너스";
            case STREAK_WEIGHT -> "체중 기록 스트릭";
            case GOAL_ACHIEVED -> "목표 달성";
            case BATTLE_COMPLETE -> "대결 완료";
            case MISSION_ATTENDANCE_WEEKLY -> "이번주 출석 미션";
            case MISSION_WEIGHT_WEEKLY -> "이번주 체중기록 미션";
            case REVIEW_AD_BONUS -> "리뷰 광고 보너스";
            case EXCHANGE -> "토스포인트로 바꾸기";
            case SIGNUP_BONUS -> "가입 축하";
            case FIRST_ATTENDANCE_BONUS -> "첫 출석 프로모션";
            case FASTING_COMPLETE -> "간헐적 단식 완료";
            case FASTING_AD_BONUS -> "간헐적 단식 광고 보너스";
            case PROFILE_BONUS -> "프로필 완성 (키)";
            case PROFILE_BONUS_GENDER -> "프로필 완성 (성별)";
            case PROFILE_BONUS_BIRTHDATE -> "프로필 완성 (생년월일)";
            case WEEKLY_MILESTONE_3 -> "이번주 체중기록 3회 달성";
            case WEEKLY_MILESTONE_5 -> "이번주 체중기록 5회 달성";
            case WEEKLY_MILESTONE_7 -> "이번주 체중기록 7회 달성";
        };
    }
}
