package com.sa.baff.api;

import com.sa.baff.model.dto.AttendanceDto;
import com.sa.baff.model.dto.RewardDto;
import com.sa.baff.service.AttendanceService;
import com.sa.baff.service.ExchangeService;
import com.sa.baff.service.RewardService;
import com.sa.baff.common.GramConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reward")
@RequiredArgsConstructor
public class RewardRestController {

    private final RewardService rewardService;
    private final ExchangeService exchangeService;
    private final AttendanceService attendanceService;

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

    /**
     * @deprecated S7-14: 구 경로 alias. 신규 경로 {@code POST /api/reward/attendance/ad-bonus} 사용 권장.
     * 다음 스프린트(S7-14-후속-A)에서 제거 예정. 구 FE 캐시/지연 배포 호환 목적.
     * body 없이 호출되므로 AdWatchEvent에는 tossAdResponse="LEGACY"로 기록된다.
     */
    @Deprecated
    @PostMapping("/attendance-ad-bonus")
    public RewardDto.rewardResponse grantAttendanceAdBonusLegacy(
            @AuthenticationPrincipal String socialId) {
        AttendanceDto.adBonusResponse response =
                attendanceService.grantAdBonus(socialId, null, "LEGACY");
        return RewardDto.rewardResponse.builder()
                .earnedGrams(response.getEarnedGrams())
                .message(GramConstants.earnMessage(response.getEarnedGrams()))
                .build();
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

    /** 간헐적 단식 완료 결과 페이지 광고 보너스 (또받기) */
    @PostMapping("/fasting-ad-bonus")
    public RewardDto.rewardResponse grantFastingAdBonus(@AuthenticationPrincipal String socialId) {
        return rewardService.grantFastingAdBonus(socialId);
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
