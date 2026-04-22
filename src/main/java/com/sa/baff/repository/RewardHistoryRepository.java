package com.sa.baff.repository;

import com.sa.baff.domain.RewardHistory;
import com.sa.baff.util.RewardStatus;
import com.sa.baff.util.RewardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RewardHistoryRepository extends JpaRepository<RewardHistory, Long> {

    List<RewardHistory> findByUserIdOrderByRegDateTimeDesc(Long userId);

    Optional<RewardHistory> findTopByUserIdAndRewardTypeOrderByRegDateTimeDesc(Long userId, RewardType rewardType);

    // S6-14: SIGNUP_BONUS 1회성 dedup — (userId, type, status, delYn) 조합 SUCCESS 이력 존재 여부
    boolean existsByUserIdAndRewardTypeAndStatusAndDelYn(
            Long userId, RewardType rewardType, RewardStatus status, Character delYn);
}
