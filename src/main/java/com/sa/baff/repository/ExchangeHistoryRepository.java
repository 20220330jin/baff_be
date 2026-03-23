package com.sa.baff.repository;

import com.sa.baff.domain.ExchangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExchangeHistoryRepository extends JpaRepository<ExchangeHistory, Long> {

    List<ExchangeHistory> findByUserIdOrderByRegDateTimeDesc(Long userId);
}
