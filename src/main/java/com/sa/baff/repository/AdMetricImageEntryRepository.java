package com.sa.baff.repository;

import com.sa.baff.domain.AdMetricImageEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AdMetricImageEntryRepository extends JpaRepository<AdMetricImageEntry, Long> {
    List<AdMetricImageEntry> findByMetricDate(LocalDate metricDate);
    List<AdMetricImageEntry> findByMetricDateBetweenOrderByMetricDateDesc(LocalDate from, LocalDate to);
}
