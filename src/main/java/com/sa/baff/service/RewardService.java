package com.sa.baff.service;

import com.sa.baff.model.dto.RewardDto;

public interface RewardService {

    /** 체중 기록 리워드 지급 */
    RewardDto.rewardResponse grantWeightReward(String socialId, Long weightId);

    /** 리뷰 작성 리워드 지급 */
    RewardDto.rewardResponse grantReviewReward(String socialId, Long reviewId);

    /** 리워드 내역 조회 */
    RewardDto.historyResponse getRewardHistory(String socialId);

    /** 포인트(그램) 잔액 조회 */
    RewardDto.pointBalanceResponse getPointBalance(String socialId);

    /** 체중 기록 광고 보너스 리워드 지급 */
    RewardDto.rewardResponse grantWeightAdBonus(String socialId, Long weightId);

    /** 출석 광고 보너스 리워드 지급 */
    RewardDto.rewardResponse grantAttendanceAdBonus(String socialId);

    /** 광고 이벤트 기록 */
    void recordAdEvent(String socialId, String watchLocation, Long referenceId, String tossAdResponse);
}
