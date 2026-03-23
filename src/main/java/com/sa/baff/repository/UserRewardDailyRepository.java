package com.sa.baff.repository;

import com.sa.baff.domain.UserRewardDaily;
import com.sa.baff.util.RewardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface UserRewardDailyRepository extends JpaRepository<UserRewardDaily, Long> {

    Optional<UserRewardDaily> findByUserIdAndRewardDateAndRewardType(
            Long userId, LocalDate rewardDate, RewardType rewardType);
}
