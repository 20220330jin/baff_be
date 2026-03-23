package com.sa.baff.repository;

import com.sa.baff.domain.RewardConfig;
import com.sa.baff.util.RewardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardConfigRepository extends JpaRepository<RewardConfig, Long> {

    List<RewardConfig> findByRewardTypeAndDelYnAndEnabledOrderByAmountAsc(
            RewardType rewardType, Character delYn, Boolean enabled);

    default List<RewardConfig> findActiveConfigs(RewardType rewardType) {
        return findByRewardTypeAndDelYnAndEnabledOrderByAmountAsc(rewardType, 'N', true);
    }
}
