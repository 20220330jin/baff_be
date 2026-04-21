package com.sa.baff.util;

/**
 * 스마트발송 대상 선별 전략 (spec §3.2).
 *
 * - ALL_TOSS_USERS: 토스 발송 가능 유저 전체 (직접 토스 + 병합 Primary). 기본값
 * - ACTIVE_LAST_7_DAYS_NOT_ATTENDED: 최근 7일 활동 + 오늘 미출석 (기존 ATTENDANCE_REMINDER 로직 보존)
 * - BALANCE_OVER_100G: 그램 100g 이상 보유 (기존 EXCHANGE_REMINDER 로직 보존)
 */
public enum SmartPushTargetStrategy {
    ALL_TOSS_USERS,
    ACTIVE_LAST_7_DAYS_NOT_ATTENDED,
    BALANCE_OVER_100G
}
