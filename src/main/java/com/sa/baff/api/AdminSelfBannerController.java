package com.sa.baff.api;

import com.sa.baff.domain.SelfBannerAd;
import com.sa.baff.repository.SelfBannerAdRepository;
import com.sa.baff.util.AdWatchLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 자체 배너광고(공지/외부앱) 어드민 CRUD.
 *
 * 인증/권한은 SecurityConfig의 /api/admin/** 가드 정책에 위임.
 */
@RestController
@RequestMapping("/api/admin/self-banners")
@RequiredArgsConstructor
public class AdminSelfBannerController {

    private final SelfBannerAdRepository selfBannerAdRepository;

    @GetMapping
    public ResponseEntity<List<SelfBannerAd>> list() {
        return ResponseEntity.ok(selfBannerAdRepository.findByDelYnOrderByPriorityAsc('N'));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        SelfBannerAd b = new SelfBannerAd();
        b.setBannerType((String) body.get("bannerType"));
        b.setPosition(AdWatchLocation.valueOf((String) body.get("position")));
        b.setTitle((String) body.get("title"));
        b.setImageUrl((String) body.get("imageUrl"));
        b.setLinkUrl((String) body.get("linkUrl"));
        if (body.get("priority") != null) b.setPriority((Integer) body.get("priority"));
        if (body.get("dailyImpressionLimit") != null) {
            b.setDailyImpressionLimit((Integer) body.get("dailyImpressionLimit"));
        }
        b.setEnabled(body.get("enabled") == null || (Boolean) body.get("enabled"));
        selfBannerAdRepository.save(b);
        return ResponseEntity.ok(Map.of("id", b.getId(), "message", "등록 완료"));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SelfBannerAd b = selfBannerAdRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("배너 없음"));
        if (body.containsKey("bannerType")) b.setBannerType((String) body.get("bannerType"));
        if (body.containsKey("position")) b.setPosition(AdWatchLocation.valueOf((String) body.get("position")));
        if (body.containsKey("title")) b.setTitle((String) body.get("title"));
        if (body.containsKey("imageUrl")) b.setImageUrl((String) body.get("imageUrl"));
        if (body.containsKey("linkUrl")) b.setLinkUrl((String) body.get("linkUrl"));
        if (body.containsKey("priority")) b.setPriority((Integer) body.get("priority"));
        if (body.containsKey("dailyImpressionLimit")) {
            b.setDailyImpressionLimit((Integer) body.get("dailyImpressionLimit"));
        }
        if (body.containsKey("enabled")) b.setEnabled((Boolean) body.get("enabled"));
        return ResponseEntity.ok(Map.of("message", "수정 완료"));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> remove(@PathVariable Long id) {
        selfBannerAdRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "삭제 완료"));
    }
}
