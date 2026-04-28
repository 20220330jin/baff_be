package com.sa.baff.model.dto;

import com.sa.baff.domain.AdMetricDailyEntry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * P0 광고전략 — 분석 탭 응답 DTO.
 *
 * spec v0.3 §3-2 카드 6장 + §3-3 일별 표.
 */
public class AdMetricAnalyticsDto {

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Kpi {
        private LocalDate metricDate;

        // 카드 6장
        /** 순손익 = 토스수익 - (그램발행 × 단가 + 토스포인트직접지급). 단가 미정 시 1원 가정 */
        private Long netProfit;
        /** 종합 eCPM = R+F+B+I 합산 (콘솔 reported) */
        private Integer totalEcpm;
        /** 활성유저(raw) — 어뷰저 필터 미적용. 정책 결정 후 자연으로 rename */
        private Long activeUsersRaw;
        private CoreActions coreActions;
        /** 실제 가입 — 당일 신규 user 수 */
        private Long newSignups;
        private GramBalance cumulativeGramBalance;

        // truth 분기 (R/F는 DB observed)
        private Long observedImpressionR;
        private Long observedImpressionF;

        // 원본 입력값 (전 truth 표시용)
        private AdMetricDailyEntry daily;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CoreActions {
        private Long weightLog;
        private Long attendance;
        private Long exchange;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class GramBalance {
        private Long totalIssued;
        private Long circulating;
        private Long holders;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DailyTableRow {
        private LocalDate metricDate;
        private AdMetricDailyEntry daily;
        private Long observedImpressionR;
        private Long observedImpressionF;
        private Long activeUsersRaw;
        private Long weightLog;
        private Long attendance;
        private Long exchange;
        private Long newSignups;
    }
}
