package com.sa.baff.api;

import com.sa.baff.domain.AdPositionConfig;
import com.sa.baff.repository.AdPositionConfigRepository;
import com.sa.baff.util.AdWatchLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ad")
@RequiredArgsConstructor
public class AdConfigRestController {

    private final AdPositionConfigRepository adPositionConfigRepository;

    /**
     * 특정 위치의 배너 광고 설정 조회 (FE용 public API)
     */
    @GetMapping("/config/{position}")
    public ResponseEntity<Map<String, Object>> getAdConfig(@PathVariable String position) {
        try {
            AdWatchLocation location = AdWatchLocation.valueOf(position);
            return adPositionConfigRepository.findByPosition(location)
                    .map(config -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("position", config.getPosition().name());
                        map.put("tossBannerAdGroupId", config.getTossBannerAdGroupId() != null ? config.getTossBannerAdGroupId() : "");
                        map.put("tossBannerAdEnabled", config.getIsTossBannerAdEnabled());
                        map.put("tossAdGroupId", config.getTossAdGroupId() != null ? config.getTossAdGroupId() : "");
                        map.put("tossAdEnabled", config.getIsTossAdEnabled());
                        map.put("tossImageAdGroupId", config.getTossImageAdGroupId() != null ? config.getTossImageAdGroupId() : "");
                        map.put("tossImageAdEnabled", config.getIsTossImageAdEnabled());
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
