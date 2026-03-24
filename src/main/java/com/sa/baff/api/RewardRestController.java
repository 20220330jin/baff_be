package com.sa.baff.api;

import com.sa.baff.model.dto.RewardDto;
import com.sa.baff.service.ExchangeService;
import com.sa.baff.service.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reward")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "changeup.reward.enabled", havingValue = "true")
public class RewardRestController {

    private final RewardService rewardService;
    private final ExchangeService exchangeService;

    /** 체중 기록 리워드 지급 */
    @PostMapping("/weight")
    public RewardDto.rewardResponse grantWeightReward(
            @AuthenticationPrincipal String socialId,
            @RequestParam Long weightId) {
        return rewardService.grantWeightReward(socialId, weightId);
    }

    /** 체중 기록 광고 보너스 리워드 지급 */
    @PostMapping("/weight-ad-bonus")
    public RewardDto.rewardResponse grantWeightAdBonus(
            @AuthenticationPrincipal String socialId,
            @RequestParam Long weightId) {
        return rewardService.grantWeightAdBonus(socialId, weightId);
    }

    /** 리뷰 작성 리워드 지급 */
    @PostMapping("/review")
    public RewardDto.rewardResponse grantReviewReward(
            @AuthenticationPrincipal String socialId,
            @RequestParam Long reviewId) {
        return rewardService.grantReviewReward(socialId, reviewId);
    }

    /** 포인트(그램) 잔액 조회 */
    @GetMapping("/point")
    public RewardDto.pointBalanceResponse getPointBalance(@AuthenticationPrincipal String socialId) {
        return rewardService.getPointBalance(socialId);
    }

    /** 리워드 적립 내역 조회 */
    @GetMapping("/history")
    public RewardDto.historyResponse getRewardHistory(@AuthenticationPrincipal String socialId) {
        return rewardService.getRewardHistory(socialId);
    }

    /** 그램 → 토스포인트 환전 */
    @PostMapping("/exchange")
    public RewardDto.exchangeResponse exchange(
            @AuthenticationPrincipal String socialId,
            @RequestBody RewardDto.exchangeRequest request) {
        return exchangeService.exchange(socialId, request.getAmount(), request.getAdWatched());
    }

    /** 광고 이벤트 기록 */
    @PostMapping("/ad-event")
    public void recordAdEvent(
            @AuthenticationPrincipal String socialId,
            @RequestBody RewardDto.adEventRequest request) {
        rewardService.recordAdEvent(socialId, request.getWatchLocation(),
                request.getReferenceId(), request.getTossAdResponse());
    }
}
