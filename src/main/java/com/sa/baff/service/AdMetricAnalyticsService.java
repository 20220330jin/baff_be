package com.sa.baff.service;

import com.sa.baff.domain.AdMetricDailyEntry;
import com.sa.baff.model.dto.AdMetricAnalyticsDto;

import java.time.LocalDate;
import java.util.List;

/**
 * P0 광고전략 — 분석 탭용 집계 service.
 *
 * spec v0.3 §3-2 KPI 카드 + §3-3 일별 KPI 표.
 * truth 룰: R/F 노출은 DB AdWatchEvent observed = truth, B/I는 콘솔 reported = truth.
 */
public interface AdMetricAnalyticsService {

    /** 단일 일자 KPI (카드 6장) */
    AdMetricAnalyticsDto.Kpi getKpi(LocalDate metricDate);

    /** 일별 KPI 표 (분석 탭 D-7) */
    List<AdMetricAnalyticsDto.DailyTableRow> getDailyTable(LocalDate from, LocalDate to);

    /** 단일 일자 분해 — AI 보고용 */
    default AdMetricDailyEntry getEntry(LocalDate metricDate) {
        AdMetricAnalyticsDto.Kpi kpi = getKpi(metricDate);
        return kpi.getDaily();
    }
}
