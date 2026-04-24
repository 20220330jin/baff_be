package com.sa.baff.config;

import com.sa.baff.domain.RewardConfig;
import com.sa.baff.repository.RewardConfigRepository;
import com.sa.baff.util.RewardType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * S6-28 주간 마일스톤 RewardConfig 초기 시딩.
 * 체중기록 3/5/7회 달성 시 각각 3g / 5g / 10g 기본값. 이미 존재하면 skip (idempotent).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(103)
public class RewardConfigWeeklyMilestoneInitializer implements ApplicationRunner {

    private final RewardConfigRepository rewardConfigRepository;

    @Override
    public void run(ApplicationArguments args) {
        seedIfAbsent(RewardType.WEEKLY_MILESTONE_3, 3, "이번주 체중기록 3회 달성 시 1회 자동 지급");
        seedIfAbsent(RewardType.WEEKLY_MILESTONE_5, 5, "이번주 체중기록 5회 달성 시 1회 자동 지급");
        seedIfAbsent(RewardType.WEEKLY_MILESTONE_7, 10, "이번주 체중기록 7회 달성 시 1회 자동 지급");
    }

    private void seedIfAbsent(RewardType type, int amount, String description) {
        boolean exists = !rewardConfigRepository
                .findByRewardTypeAndDelYnAndEnabledOrderByAmountAsc(type, 'N', true).isEmpty()
                || !rewardConfigRepository
                .findByRewardTypeAndDelYnAndEnabledOrderByAmountAsc(type, 'N', false).isEmpty();
        if (exists) {
            log.info("[RewardConfig.{}] 기존 설정 존재 — 시딩 생략", type);
            return;
        }

        RewardConfig config = new RewardConfig();
        config.setRewardType(type);
        config.setAmount(amount);
        config.setProbability(100);
        config.setDailyLimit(1);
        config.setIsFixed(true);
        config.setEnabled(true);
        config.setDescription(description);
        rewardConfigRepository.save(config);

        log.info("[RewardConfig.{}] 초기 시딩 완료 (amount={}g, enabled=true)", type, amount);
    }
}
