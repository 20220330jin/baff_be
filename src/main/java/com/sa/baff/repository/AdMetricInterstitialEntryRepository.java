package com.sa.baff.repository;

import com.sa.baff.domain.AdMetricInterstitialEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AdMetricInterstitialEntryRepository extends JpaRepository<AdMetricInterstitialEntry, Long> {
    List<AdMetricInterstitialEntry> findByMetricDate(LocalDate metricDate);
    List<AdMetricInterstitialEntry> findByMetricDateBetweenOrderByMetricDateDesc(LocalDate from, LocalDate to);
}
