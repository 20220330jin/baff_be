package com.sa.baff.service;

import com.sa.baff.domain.FastingRecord;
import com.sa.baff.util.FastingEndStatus;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 간헐적 단식 종료 판정 규칙 (M-043 · spec specs/2026-11-17-fasting-end-recovery-design.md).
 * 현재 시각을 인자로 받아 경계값을 테스트로 고정한다.
 */
public final class FastingEndPolicy {

    /** 진행 상한 48시간 — 넘으면 「종료 시각 확인 필요」 */
    public static final int MAX_FASTING_MINUTES = 48 * 60;
    public static final int MAX_FASTING_HOURS = MAX_FASTING_MINUTES / 60;
    /** 사용자 지정 목표 상한 (상한 48시간보다 짧아야 한다) */
    public static final int CUSTOM_MAX_HOURS = 36;
    private static final int CUSTOM_MIN_HOURS = 1;
    /** 복구 시각은 시작 후 최소 1분 */
    private static final int MIN_FASTING_MINUTES = 1;

    private FastingEndPolicy() {
    }

    /** 경과 분 (초 이하 버림) */
    public static long elapsedMinutes(LocalDateTime start, LocalDateTime now) {
        return Duration.between(start, now).toMinutes();
    }

    /** 경과가 상한을 넘었으면 true — 지금 시각으로 종료할 수 없다 */
    public static boolean needsEndTime(LocalDateTime start, LocalDateTime now) {
        return elapsedMinutes(start, now) > MAX_FASTING_MINUTES;
    }

    /** 복구 시각 검사: 시작 + 1분 ~ 시작 + 48시간 · 지금 이하 */
    public static void validateEndTime(LocalDateTime start, LocalDateTime endTime, LocalDateTime now) {
        if (endTime.isAfter(now)) {
            throw new IllegalArgumentException("지금보다 늦은 시각은 고를 수 없어요.");
        }
        long minutes = elapsedMinutes(start, endTime);
        if (minutes < MIN_FASTING_MINUTES) {
            throw new IllegalArgumentException("단식 시작 뒤의 시각을 골라 주세요.");
        }
        if (minutes > MAX_FASTING_MINUTES) {
            throw new IllegalArgumentException("시작 후 " + MAX_FASTING_HOURS + "시간 안의 시각을 골라 주세요.");
        }
    }

    /** 통계 · AI 분석 대상인가 — 종료됐고 확인된 기록만 */
    public static boolean countsInStats(FastingRecord r) {
        return r.getEndTime() != null
                && (r.getEndStatus() == null || r.getEndStatus() == FastingEndStatus.CONFIRMED);
    }

    /** 화면 · 응답에 내려줄 상태 — 종료 시각이 있고 상태가 비어 있으면 CONFIRMED */
    public static FastingEndStatus effectiveStatus(FastingRecord r) {
        if (r.getEndStatus() != null) {
            return r.getEndStatus();
        }
        return r.getEndTime() != null ? FastingEndStatus.CONFIRMED : null;
    }

    /** 사용자 지정 목표 1 ~ 36시간 */
    public static void validateCustomHours(Integer hours) {
        if (hours == null || hours < CUSTOM_MIN_HOURS || hours > CUSTOM_MAX_HOURS) {
            throw new IllegalArgumentException("단식 시간은 1 ~ 36시간으로 정해 주세요.");
        }
    }
}
