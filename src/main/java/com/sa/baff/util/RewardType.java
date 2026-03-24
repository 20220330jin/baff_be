package com.sa.baff.util;

public enum RewardType {
    // Phase 1
    WEIGHT_LOG,              // 체중 기록
    REVIEW,                  // 리뷰 작성
    ATTENDANCE,              // 출석
    ATTENDANCE_STREAK,       // 연속 출석 보너스
    ATTENDANCE_AD_BONUS,     // 출석 광고 보너스
    WEIGHT_AD_BONUS,         // 체중 기록 광고 보너스

    // Phase 2 (예약)
    STREAK_WEIGHT,           // 체중 기록 스트릭
    GOAL_ACHIEVED,           // 목표 달성
    BATTLE_COMPLETE          // 배틀 완료 참가보상
}
