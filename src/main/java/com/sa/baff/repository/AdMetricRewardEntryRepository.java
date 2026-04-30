package com.sa.baff.repository;

import com.sa.baff.domain.AdMetricRewardEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AdMetricRewardEntryRepository extends JpaRepository<AdMetricRewardEntry, Long> {
    List<AdMetricRewardEntry> findByMetricDate(LocalDate metricDate);
    List<AdMetricRewardEntry> findByMetricDateBetweenOrderByMetricDateDesc(LocalDate from, LocalDate to);
}
