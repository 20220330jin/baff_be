package com.sa.baff.api;

import com.sa.baff.domain.AdPositionConfig;
import com.sa.baff.domain.SelfBannerAd;
import com.sa.baff.domain.UserB;
import com.sa.baff.repository.AdPositionConfigRepository;
import com.sa.baff.repository.AdWatchEventRepository;
import com.sa.baff.repository.SelfBannerAdRepository;
import com.sa.baff.repository.UserRepository;
import com.sa.baff.util.AdWatchLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ad")
@RequiredArgsConstructor
public class AdConfigRestController {

    private final AdPositionConfigRepository adPositionConfigRepository;
    private final SelfBannerAdRepository selfBannerAdRepository;
    private final AdWatchEventRepository adWatchEventRepository;
    private final UserRepository userRepository;

    /**
     * 특정 위치의 배너 광고 설정 조회 (FE용 public API).
     *
     * 응답에 추가 필드:
     *  - hasSelfBanners: 활성 자체배너 존재 여부 (FE에서 자체배너 우선 노출 판단)
     *  - limitReached: 토스 광고 빈도 제한 도달 여부 (frequencyLimitEnabled + dailyImpressionLimit 체크)
     *
     * 나만그래 패턴: 자체배너 우선 노출, 한도 도달 시 토스 차단 → 자체배너 fallback.
     */
    @GetMapping("/config/{position}")
    public ResponseEntity<Map<String, Object>> getAdConfig(
            @PathVariable String position,
            @AuthenticationPrincipal String socialId) {
        try {
            AdWatchLocation location = AdWatchLocation.valueOf(position);
            return adPositionConfigRepository.findByPosition(location)
                    .map(config -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("position", config.getPosition().name());
                        map.put("tossBannerAdGroupId", config.getTossBannerAdGroupId() != null ? config.getTossBannerAdGroupId() : "");
                        map.put("tossBannerAdEnabled", config.getIsTossBannerAdEnabled());
                        map.put("tossBannerAdRatio", config.getTossBannerAdRatio() != null ? config.getTossBannerAdRatio() : 100);
                        map.put("tossAdGroupId", config.getTossAdGroupId() != null ? config.getTossAdGroupId() : "");
                        map.put("tossAdEnabled", config.getIsTossAdEnabled());
                        map.put("tossImageAdGroupId", config.getTossImageAdGroupId() != null ? config.getTossImageAdGroupId() : "");
                        map.put("tossImageAdEnabled", config.getIsTossImageAdEnabled());
                        map.put("tossImageAdRatio", config.getTossImageAdRatio() != null ? config.getTossImageAdRatio() : 0);

                        // 자체배너 우선 노출 신호
                        List<SelfBannerAd> selfBanners = selfBannerAdRepository
                                .findByPositionAndEnabledAndDelYnOrderByPriorityAsc(location, true, 'N');
                        map.put("hasSelfBanners", !selfBanners.isEmpty());

                        // 빈도 제한 enforce — 유저별 오늘 토스 광고 노출 수 vs dailyImpressionLimit
                        boolean limitReached = false;
                        if (Boolean.TRUE.equals(config.getFrequencyLimitEnabled())
                                && config.getDailyImpressionLimit() != null
                                && config.getDailyImpressionLimit() > 0
                                && socialId != null) {
                            UserB user = userRepository.findBySocialId(socialId).orElse(null);
                            if (user != null) {
                                LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
                                LocalDateTime endOfDay = startOfDay.plusDays(1);
                                long todayCount = adWatchEventRepository
                                        .countByUserIdAndWatchLocationAndRegDateTimeBetween(
                                                user.getId(), location, startOfDay, endOfDay);
                                limitReached = todayCount >= config.getDailyImpressionLimit();
                            }
                        }
                        map.put("limitReached", limitReached);

                        return ResponseEntity.ok(map);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 리워드/전면 광고 설정 일괄 조회 (FE useRewardedAd hook용)
     * 나만그래 패턴: location → config map
     */
    @GetMapping("/reward-ad-config")
    public ResponseEntity<Map<String, Map<String, Object>>> getRewardAdConfigs() {
        List<AdPositionConfig> configs = adPositionConfigRepository.findAll();
        Map<String, Map<String, Object>> result = new HashMap<>();

        for (AdPositionConfig c : configs) {
            Map<String, Object> item = new HashMap<>();
            item.put("tossRewardedAdGroupId", c.getTossAdGroupId());
            item.put("isRewardedAdEnabled", c.getIsTossAdEnabled());
            item.put("tossInterstitialAdGroupId", c.getTossInterstitialAdGroupId());
            item.put("isInterstitialAdEnabled", c.getIsTossInterstitialAdEnabled());
            item.put("rewardedAdRatio", c.getRewardedAdRatio());
            item.put("rewardedAdGrams", c.getRewardedAdGrams());
            item.put("interstitialAdGrams", c.getInterstitialAdGrams());
            result.put(c.getPosition().name(), item);
        }

        return ResponseEntity.ok(result);
    }
}
