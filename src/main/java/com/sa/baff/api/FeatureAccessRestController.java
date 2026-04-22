package com.sa.baff.api;

import com.sa.baff.domain.FeatureAccessConfig;
import com.sa.baff.repository.FeatureAccessConfigRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * S6-2 — 기능별 접근 제어 API.
 *
 *  - GET /api/config/feature-access       (public, FE 앱 시작 시 bulk 조회)
 *  - GET /api/admin/config/feature-access (ADMIN, 어드민 화면 목록)
 *  - PUT /api/admin/config/feature-access/{key} (ADMIN, 개별 토글)
 */
@RestController
@RequiredArgsConstructor
public class FeatureAccessRestController {

    private final FeatureAccessConfigRepository repository;

    @GetMapping("/api/config/feature-access")
    public ResponseEntity<Map<String, FeatureAccessItem>> getAll() {
        Map<String, FeatureAccessItem> result = new HashMap<>();
        repository.findAll().forEach(config ->
                result.put(config.getFeatureKey(),
                        new FeatureAccessItem(config.isEnabled(), config.isLoginRequired(), config.getDescription()))
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/admin/config/feature-access")
    public ResponseEntity<Iterable<FeatureAccessConfig>> getAllForAdmin() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PutMapping("/api/admin/config/feature-access/{key}")
    public ResponseEntity<?> update(@PathVariable String key, @RequestBody UpdateRequest request) {
        FeatureAccessConfig config = repository.findByFeatureKey(key)
                .orElseThrow(() -> new IllegalArgumentException("설정을 찾을 수 없습니다: " + key));
        config.setEnabled(request.isEnabled());
        config.setLoginRequired(request.isLoginRequired());
        repository.save(config);
        return ResponseEntity.ok(Map.of(
                "message", key + " 설정 변경 완료",
                "enabled", request.isEnabled(),
                "loginRequired", request.isLoginRequired()
        ));
    }

    @Getter
    private static class FeatureAccessItem {
        private final boolean enabled;
        private final boolean loginRequired;
        private final String description;

        FeatureAccessItem(boolean enabled, boolean loginRequired, String description) {
            this.enabled = enabled;
            this.loginRequired = loginRequired;
            this.description = description;
        }
    }

    @Getter
    private static class UpdateRequest {
        private boolean enabled;
        private boolean loginRequired;
    }
}
