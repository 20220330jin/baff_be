package com.sa.baff.repository;

import com.sa.baff.domain.RewardHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardHistoryRepository extends JpaRepository<RewardHistory, Long> {

    List<RewardHistory> findByUserIdOrderByRegDateTimeDesc(Long userId);
}
