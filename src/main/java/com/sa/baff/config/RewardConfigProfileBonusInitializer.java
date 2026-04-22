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
 * S6-15 PROFILE_BONUS RewardConfig 초기 시딩.
 * PROFILE_BONUS row가 없을 때만 amount=1, enabled=true 기본값 생성.
 * 이미 존재하면 skip (idempotent) — RewardConfigSignupBonusInitializer 패턴 준용.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(102)
public class RewardConfigProfileBonusInitializer implements ApplicationRunner {

    private final RewardConfigRepository rewardConfigRepository;

    @Override
    public void run(ApplicationArguments args) {
        boolean exists = !rewardConfigRepository
                .findByRewardTypeAndDelYnAndEnabledOrderByAmountAsc(
                        RewardType.PROFILE_BONUS, 'N', true).isEmpty()
                || !rewardConfigRepository
                .findByRewardTypeAndDelYnAndEnabledOrderByAmountAsc(
                        RewardType.PROFILE_BONUS, 'N', false).isEmpty();
        if (exists) {
            log.info("[RewardConfig.PROFILE_BONUS] 기존 설정 존재 — 시딩 생략");
            return;
        }

        RewardConfig config = new RewardConfig();
        config.setRewardType(RewardType.PROFILE_BONUS);
        config.setAmount(1);
        config.setProbability(100);
        config.setDailyLimit(1);
        config.setIsFixed(true);
        config.setEnabled(true);
        config.setDescription("최초 height(키) 입력 시 1회 자동 지급");
        rewardConfigRepository.save(config);

        log.info("[RewardConfig.PROFILE_BONUS] 초기 시딩 완료 (amount=1, enabled=true)");
    }
}
