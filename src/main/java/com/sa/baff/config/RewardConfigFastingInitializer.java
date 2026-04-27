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
 * 간헐적 단식 RewardConfig 시드 (FASTING_COMPLETE, FASTING_AD_BONUS).
 * 둘 다 amount=1, dailyLimit=1, enabled=true 기본.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(104)
public class RewardConfigFastingInitializer implements ApplicationRunner {

    private final RewardConfigRepository rewardConfigRepository;

    @Override
    public void run(ApplicationArguments args) {
        try {
            seedIfAbsent(RewardType.FASTING_COMPLETE, "간헐적 단식 목표 시간 완료 시 자동 지급");
            seedIfAbsent(RewardType.FASTING_AD_BONUS, "간헐적 단식 완료 결과 페이지 광고 시청 보너스");
        } catch (Exception e) {
            log.warn("[RewardConfig.FASTING_*] 시딩 실패 (부팅 계속): {}", e.getMessage());
        }
    }

    private void seedIfAbsent(RewardType type, String description) {
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
        config.setAmount(1);
        config.setProbability(100);
        config.setDailyLimit(1);
        config.setIsFixed(true);
        config.setEnabled(true);
        config.setDescription(description);
        rewardConfigRepository.save(config);
        log.info("[RewardConfig.{}] 시드 완료 (amount=1, enabled=true)", type);
    }
}
