package com.sa.baff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 계정 통합 API 활성화 플래그.
 *
 * Plan v3 Task 1.5-9 (Plan Review Round 3 P0) — Phase 1.5 BE가 FE보다 먼저 merge/deploy될 때
 * irreversible merge API가 FE 동의 화면 없이 프로덕션에 노출되지 않도록 기본 disabled.
 *
 * 활성화 조건 (application-prod.yml에서 명시적으로 true 설정):
 *   1. Phase 1.5 BE merge + CP2 Round 3 Sign-off
 *   2. Phase 2 FE 구현 완료
 *   3. CP2-FE Sign-off
 *   4. 내부 테스트 통과
 *   5. 대표님 릴리즈 승인
 */
@ConfigurationProperties(prefix = "baff.account-link")
public record AccountLinkFeatureProperties(boolean enabled) {

    public AccountLinkFeatureProperties() {
        this(false);
    }
}
