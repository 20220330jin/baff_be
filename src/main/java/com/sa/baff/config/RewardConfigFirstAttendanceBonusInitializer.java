package com.sa.baff.config;

import com.sa.baff.common.GramConstants;
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
 * FIRST_ATTENDANCE_BONUS RewardConfig 초기 시딩 (첫 출석 프로모션, 토스포인트 직접 지급).
 *
 * 토스 콘솔 승인 + 프로모션 코드 발급 전까지 enabled=false, promotionCode=null로 시드.
 * 운영 시 어드민에서 promotionCode 입력 + enabled=true 토글하면 즉시 활성화.
 *
 * 나만그래 FIRST_VOTE_BONUS 패턴 정렬.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(103)
public class RewardConfigFirstAttendanceBonusInitializer implements ApplicationRunner {

    private final RewardConfigRepository rewardConfigRepository;

    @Override
    public void run(ApplicationArguments args) {
        boolean exists = !rewardConfigRepository
                .findByRewardTypeAndDelYnAndEnabledOrderByAmountAsc(
                        RewardType.FIRST_ATTENDANCE_BONUS, 'N', true).isEmpty()
                || !rewardConfigRepository
                .findByRewardTypeAndDelYnAndEnabledOrderByAmountAsc(
                        RewardType.FIRST_ATTENDANCE_BONUS, 'N', false).isEmpty();
        if (exists) {
            log.info("[RewardConfig.FIRST_ATTENDANCE_BONUS] 기존 설정 존재 — 시딩 생략");
            return;
        }

        RewardConfig config = new RewardConfig();
        config.setRewardType(RewardType.FIRST_ATTENDANCE_BONUS);
        config.setAmount(GramConstants.FIRST_ATTENDANCE_TOSS_POINT_AMOUNT);
        config.setProbability(100);
        config.setDailyLimit(1);
        config.setIsFixed(true);
        config.setEnabled(false);
        config.setPromotionCode(null);
        config.setDescription("첫 출석 시 토스포인트 " + GramConstants.FIRST_ATTENDANCE_TOSS_POINT_AMOUNT
                + "원 직접 지급. 어드민에서 promotionCode 입력 + enabled=true 토글 시 활성화.");
        rewardConfigRepository.save(config);

        log.info("[RewardConfig.FIRST_ATTENDANCE_BONUS] 초기 시딩 완료 (amount={}, enabled=false, 활성화 대기)",
                GramConstants.FIRST_ATTENDANCE_TOSS_POINT_AMOUNT);
    }
}
