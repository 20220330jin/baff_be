package com.sa.baff.repository;

import com.sa.baff.domain.AdMetricBannerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AdMetricBannerEntryRepository extends JpaRepository<AdMetricBannerEntry, Long> {
    List<AdMetricBannerEntry> findByMetricDate(LocalDate metricDate);
    List<AdMetricBannerEntry> findByMetricDateBetweenOrderByMetricDateDesc(LocalDate from, LocalDate to);
}
