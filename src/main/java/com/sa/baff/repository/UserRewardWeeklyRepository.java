package com.sa.baff.repository;

import com.sa.baff.domain.UserRewardWeekly;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface UserRewardWeeklyRepository extends JpaRepository<UserRewardWeekly, Long> {

    Optional<UserRewardWeekly> findByUserIdAndWeekStartDate(Long userId, LocalDate weekStartDate);
}
