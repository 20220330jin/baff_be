package com.sa.baff.repository;

import com.sa.baff.domain.AdMetricDailyEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AdMetricDailyEntryRepository extends JpaRepository<AdMetricDailyEntry, Long> {

    Optional<AdMetricDailyEntry> findByMetricDate(LocalDate metricDate);

    /** 분석 탭 D-7 일별 표 — 최신순 */
    List<AdMetricDailyEntry> findByMetricDateBetweenOrderByMetricDateDesc(LocalDate from, LocalDate to);
}
