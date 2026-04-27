package com.sa.baff.service;

import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.RewardDto;
import com.sa.baff.util.RewardType;

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

    /**
     * 프로필 완성 보너스 지급 (필드별 최초 1회, dedup 내부 처리).
     * S6-15(height) / S6-30(gender, birthdate) 공용.
     * @param rewardType PROFILE_BONUS | PROFILE_BONUS_GENDER | PROFILE_BONUS_BIRTHDATE
     */
    /** @return 지급된 그램 (0이면 비활성/이미 지급/실패). */
    int claimProfileBonus(Long userId, UserB user, RewardType rewardType);

    /**
     * S6-28 주간 마일스톤 체크 & 지급.
     * 이번주 체중기록 SUCCESS 카운트가 3/5/7회 도달 시 각각 1회 자동 지급. dedup은 UserRewardWeekly 플래그.
     * grantWeightReward 등 WEIGHT_LOG 지급 직후 호출 — 예외는 내부 swallow.
     */
    void claimWeeklyMilestones(Long userId, UserB user);

    /**
     * 가입 축하 보너스 지급 (그램, 가입 즉시 1회).
     * - RewardConfig.SIGNUP_BONUS 활성화 시 지급
     * - 동일 유저 SUCCESS 이력 있으면 skip
     * - 예외 swallow (호출자 가입 경로 영향 없도록)
     */
    void claimSignupBonus(Long userId, UserB user);

    /**
     * 첫 출석 프로모션 (토스포인트 직접 지급, 1회).
     * - RewardConfig.FIRST_ATTENDANCE_BONUS 활성화 + promotionCode 설정 시 지급
     * - 동일 유저 SUCCESS/PENDING 이력 있으면 skip (생애 1회 보장)
     * - 토스 API 실패 시 RewardHistory FAILED 기록 + 예외 swallow
     */
    void claimFirstAttendanceBonus(Long userId, String socialId);

    /** 간헐적 단식 완료 리워드 (목표 달성 시 자동 지급). */
    RewardDto.rewardResponse grantFastingCompleteReward(String socialId, Long fastingRecordId);

    /** 간헐적 단식 완료 결과 페이지 광고 보너스. */
    RewardDto.rewardResponse grantFastingAdBonus(String socialId);
}
