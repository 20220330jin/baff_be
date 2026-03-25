package com.sa.baff.api;

import com.sa.baff.domain.AdPositionConfig;
import com.sa.baff.repository.AdPositionConfigRepository;
import com.sa.baff.util.AdWatchLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
                    .map(config -> ResponseEntity.ok(Map.<String, Object>of(
                            "position", config.getPosition().name(),
                            "tossBannerAdGroupId", config.getTossBannerAdGroupId() != null ? config.getTossBannerAdGroupId() : "",
                            "tossBannerAdEnabled", config.getIsTossBannerAdEnabled(),
                            "tossAdGroupId", config.getTossAdGroupId() != null ? config.getTossAdGroupId() : "",
                            "tossAdEnabled", config.getIsTossAdEnabled()
                    )))
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
