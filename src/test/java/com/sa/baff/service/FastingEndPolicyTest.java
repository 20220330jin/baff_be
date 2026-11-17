package com.sa.baff.service;

import com.sa.baff.domain.FastingRecord;
import com.sa.baff.util.FastingEndStatus;
import com.sa.baff.util.FastingMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** M-043 판정 규칙 경계값 (spec §5 시간 경계 · §7 수용기준 1 · 2 · 7 · 11) */
class FastingEndPolicyTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 1, 20, 0);

    @Test
    @DisplayName("경과 2,880분(48시간)은 지금 종료 가능, 2,881분부터 확인 필요, 13일이면 확인 필요")
    void needsEndTime_boundary() {
        assertThat(FastingEndPolicy.needsEndTime(START, START.plusMinutes(2880))).isFalse();
        assertThat(FastingEndPolicy.needsEndTime(START, START.plusMinutes(2881))).isTrue();
        assertThat(FastingEndPolicy.needsEndTime(START, START.plusDays(13))).isTrue();
        assertThat(FastingEndPolicy.needsEndTime(START, START.plusMinutes(2880).plusSeconds(59))).isFalse();
    }

    @Test
    @DisplayName("복구 시각: 시작+1분 ~ 시작+48시간 · 지금 이하만 통과")
    void validateEndTime_boundary() {
        LocalDateTime now = START.plusDays(13);
        assertThatThrownBy(() -> FastingEndPolicy.validateEndTime(START, START, now))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FastingEndPolicy.validateEndTime(START, START.minusMinutes(1), now))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> FastingEndPolicy.validateEndTime(START, START.plusMinutes(1), now)).doesNotThrowAnyException();
        assertThatCode(() -> FastingEndPolicy.validateEndTime(START, START.plusMinutes(990), now)).doesNotThrowAnyException();
        assertThatCode(() -> FastingEndPolicy.validateEndTime(START, START.plusMinutes(2880), now)).doesNotThrowAnyException();
        assertThatThrownBy(() -> FastingEndPolicy.validateEndTime(START, START.plusMinutes(2881), now))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("복구 시각이 지금보다 늦으면 거절 (시작 20시간 뒤에 21시간 지점 입력)")
    void validateEndTime_future() {
        LocalDateTime now = START.plusHours(20);
        assertThatThrownBy(() -> FastingEndPolicy.validateEndTime(START, now.plusMinutes(1), now))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> FastingEndPolicy.validateEndTime(START, now, now)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("통계 대상: 종료 + (상태 없음 | CONFIRMED) 만")
    void countsInStats() {
        assertThat(FastingEndPolicy.countsInStats(record(START.plusHours(16), null))).isTrue();
        assertThat(FastingEndPolicy.countsInStats(record(START.plusHours(16), FastingEndStatus.CONFIRMED))).isTrue();
        assertThat(FastingEndPolicy.countsInStats(record(START.plusHours(319), FastingEndStatus.NEEDS_CHECK))).isFalse();
        assertThat(FastingEndPolicy.countsInStats(record(null, FastingEndStatus.UNKNOWN))).isFalse();
        assertThat(FastingEndPolicy.countsInStats(record(null, null))).isFalse();
    }

    @Test
    @DisplayName("응답 상태: 종료 시각이 있고 상태가 비면 CONFIRMED, 진행 중은 null")
    void effectiveStatus() {
        assertThat(FastingEndPolicy.effectiveStatus(record(START.plusHours(16), null))).isEqualTo(FastingEndStatus.CONFIRMED);
        assertThat(FastingEndPolicy.effectiveStatus(record(null, FastingEndStatus.UNKNOWN))).isEqualTo(FastingEndStatus.UNKNOWN);
        assertThat(FastingEndPolicy.effectiveStatus(record(null, null))).isNull();
    }

    @Test
    @DisplayName("사용자 지정 목표 1 ~ 36시간")
    void validateCustomHours() {
        assertThatCode(() -> FastingEndPolicy.validateCustomHours(1)).doesNotThrowAnyException();
        assertThatCode(() -> FastingEndPolicy.validateCustomHours(36)).doesNotThrowAnyException();
        for (Integer bad : new Integer[]{null, 0, 37, 72}) {
            assertThatThrownBy(() -> FastingEndPolicy.validateCustomHours(bad))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    private FastingRecord record(LocalDateTime endTime, FastingEndStatus status) {
        FastingRecord r = FastingRecord.builder()
                .startTime(START).endTime(endTime).mode(FastingMode.SIXTEEN_EIGHT).targetHours(16).build();
        r.setEndStatus(status);
        return r;
    }
}
