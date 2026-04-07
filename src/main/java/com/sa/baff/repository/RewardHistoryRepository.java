package com.sa.baff.repository;

import com.sa.baff.domain.RewardHistory;
import com.sa.baff.util.RewardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RewardHistoryRepository extends JpaRepository<RewardHistory, Long> {

    List<RewardHistory> findByUserIdOrderByRegDateTimeDesc(Long userId);

    Optional<RewardHistory> findTopByUserIdAndRewardTypeOrderByRegDateTimeDesc(Long userId, RewardType rewardType);
}
