package com.sa.baff.service;

import com.sa.baff.common.GramConstants;
import com.sa.baff.domain.*;
import com.sa.baff.model.dto.RewardDto;
import com.sa.baff.repository.*;
import com.sa.baff.util.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
@ConditionalOnProperty(name = "changeup.reward.enabled", havingValue = "true")
public class RewardServiceImpl implements RewardService {

    private final UserRepository userRepository;
    private final PieceRepository pieceRepository;
    private final PieceTransactionRepository pieceTransactionRepository;
    private final RewardConfigRepository rewardConfigRepository;
    private final RewardHistoryRepository rewardHistoryRepository;
    private final UserRewardDailyRepository userRewardDailyRepository;
    private final AdWatchEventRepository adWatchEventRepository;

    private final Random random = new Random();

    @Override
    public RewardDto.rewardResponse grantWeightReward(String socialId, Long weightId) {
        UserB user = findUser(socialId);
        Long userId = user.getId();

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

    // === private helpers ===

    private UserB findUser(String socialId) {
        return userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N')
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
                .orElse(1); // 기본 일 1회

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
            case STREAK_WEIGHT -> "체중 기록 스트릭";
            case GOAL_ACHIEVED -> "목표 달성";
            case BATTLE_COMPLETE -> "배틀 완료";
        };
    }
}
