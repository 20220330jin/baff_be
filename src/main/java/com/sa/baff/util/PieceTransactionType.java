package com.sa.baff.util;

public enum PieceTransactionType {
    // === 배틀 (기존) ===
    DEPOSIT,        // 지급 (관리자/보상)
    BET_DEDUCT,     // 내기 차감 (배틀 시작 시)
    BET_WIN,        // 내기 승리 (상대 조각 획득)
    BET_REFUND,     // 내기 환불 (무승부/취소)

    // === 리워드 (Phase 1) ===
    REWARD_WEIGHT_LOG,       // 체중 기록 리워드
    REWARD_REVIEW,           // 리뷰 작성 리워드
    REWARD_ATTENDANCE,       // 출석 리워드
    REWARD_STREAK_ATTENDANCE,// 연속 출석 보너스
    REWARD_AD_BONUS,         // 광고 보너스 (광고 시청 추가 적립)
    REWARD_MISSION,          // 미션 완료 리워드
    REWARD_SIGNUP_BONUS,     // S6-14 가입 축하 리워드 (1회성)
    REWARD_PROFILE_BONUS,              // S6-15 프로필 완성 리워드 - 키 (최초 height 입력 시 1회성)
    REWARD_PROFILE_BONUS_GENDER,       // S6-30 프로필 완성 리워드 - 성별 (최초 gender 입력 시 1회성)
    REWARD_PROFILE_BONUS_BIRTHDATE,    // S6-30 프로필 완성 리워드 - 생년월일 (최초 birthdate 입력 시 1회성)
    REWARD_WEEKLY_MILESTONE_3,         // S6-28 주간 마일스톤 리워드 - 3회
    REWARD_WEEKLY_MILESTONE_5,         // S6-28 주간 마일스톤 리워드 - 5회
    REWARD_WEEKLY_MILESTONE_7,         // S6-28 주간 마일스톤 리워드 - 7회

    // === 환전 (Phase 1) ===
    EXCHANGE_REQUEST,        // 환전 요청 (차감)
    EXCHANGE_SUCCESS,        // 환전 성공
    EXCHANGE_FAILED_REFUND   // 환전 실패 환불
}
