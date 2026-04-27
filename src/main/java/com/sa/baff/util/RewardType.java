package com.sa.baff.util;

public enum RewardType {
    // Phase 1
    WEIGHT_LOG,              // 체중 기록
    REVIEW,                  // 리뷰 작성
    ATTENDANCE,              // 출석
    ATTENDANCE_STREAK,       // 연속 출석 보너스
    ATTENDANCE_AD_BONUS,     // 출석 광고 보너스
    WEIGHT_AD_BONUS,         // 체중 기록 광고 보너스
    SIGNUP_BONUS,            // S6-14 가입 축하 (가입 즉시 1회 지급, 그램)
    FIRST_ATTENDANCE_BONUS,  // S7-X 첫 출석 프로모션 (첫 출석 시 토스포인트 직접 지급, RewardConfig.enabled+promotionCode 시 활성화)
    PROFILE_BONUS,           // S6-15 프로필 완성 보너스 - 키 (최초 height 입력 시 1회 지급)
    PROFILE_BONUS_GENDER,    // S6-30 프로필 완성 보너스 - 성별 (최초 gender 입력 시 1회 지급)
    PROFILE_BONUS_BIRTHDATE, // S6-30 프로필 완성 보너스 - 생년월일 (최초 birthdate 입력 시 1회 지급)

    // Phase 2 (예약)
    STREAK_WEIGHT,           // 체중 기록 스트릭
    GOAL_ACHIEVED,           // 목표 달성
    BATTLE_COMPLETE,         // 배틀 완료 참가보상

    // 미션
    MISSION_ATTENDANCE_WEEKLY,  // 이번주 미션: 출석 4일
    MISSION_WEIGHT_WEEKLY,      // 이번주 미션: 체중기록 3일

    // 광고 보너스
    REVIEW_AD_BONUS,         // 리뷰 광고 보너스

    // 간헐적 단식
    FASTING_COMPLETE,        // 간헐적 단식 목표 시간 완료 시 자동 지급
    FASTING_AD_BONUS,        // 간헐적 단식 완료 결과 페이지 광고 보너스

    // S6-28 주간 마일스톤 (체중기록 횟수 기반 자동 백그라운드 보상)
    WEEKLY_MILESTONE_3,      // 이번주 체중기록 3회 달성
    WEEKLY_MILESTONE_5,      // 이번주 체중기록 5회 달성
    WEEKLY_MILESTONE_7,      // 이번주 체중기록 7회 달성

    // 환전
    EXCHANGE                 // 환전 (그램 차감)
}
