package com.sa.baff.api;

import com.sa.baff.domain.SelfBannerAd;
import com.sa.baff.repository.SelfBannerAdRepository;
import com.sa.baff.util.AdWatchLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 자체 배너광고 공개 조회 (FE fallback용).
 *
 * 토스 광고가 비활성/no-fill일 때 위치별 enabled=true 자체 배너 노출.
 * priority 오름차순 정렬, FE에서 첫 항목 또는 random 선택.
 */
@RestController
@RequestMapping("/api/self-banners")
@RequiredArgsConstructor
public class SelfBannerController {

    private final SelfBannerAdRepository selfBannerAdRepository;

    @GetMapping("/{position}")
    public ResponseEntity<List<SelfBannerAd>> byPosition(@PathVariable String position) {
        try {
            AdWatchLocation loc = AdWatchLocation.valueOf(position);
            List<SelfBannerAd> banners = selfBannerAdRepository
                    .findByPositionAndEnabledAndDelYnOrderByPriorityAsc(loc, true, 'N');
            return ResponseEntity.ok(banners);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
