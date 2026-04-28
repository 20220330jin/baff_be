package com.sa.baff.repository;

import com.sa.baff.domain.AdMetricEntryRevisionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AdMetricEntryRevisionLogRepository extends JpaRepository<AdMetricEntryRevisionLog, Long> {

    /** 특정 row의 변경 이력 — 최신순 */
    List<AdMetricEntryRevisionLog> findByTableNameAndRowMetricDateOrderByIdDesc(
            String tableName, LocalDate rowMetricDate);
}
