package com.sa.baff.util;

/**
 * 단식 기록 종료 상태 (M-043). 진행 중이면 null.
 * 종료 시각이 있고 상태가 null 이면 CONFIRMED 로 읽는다 — 배포 뒤 마이그레이션 전 기록.
 */
public enum FastingEndStatus {
    /** 정상 종료 · 식사 시각 입력으로 복구 — 통계 · 보상 포함 */
    CONFIRMED,
    /** 종료 시각 미확인(「기억 안 나요」) — 종료 시각 없음 · 통계 · 보상 제외 */
    UNKNOWN,
    /** 기존 48시간 초과 기록 — 원래 값 보존 · 통계 제외 */
    NEEDS_CHECK
}
