package com.sa.baff.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * S6-2 — 기능별 접근 제어 설정 (나만그래 참조, 체인지업 확장판).
 *
 * featureKey 별 1행 (UNIQUE).
 *  - enabled: 기능 전체 ON/OFF (AI/베타 긴급 차단용)
 *  - loginRequired: 로그인 필수 여부 (공개 라우트 중 일부 기능 선택적 요구)
 *
 * 어드민에서 런타임 토글 — 배포 없이 기능 활성/비활성 및 가드 조정.
 */
@Entity
@Table(name = "feature_access_config", uniqueConstraints = {
        @UniqueConstraint(columnNames = "feature_key")
})
@NoArgsConstructor
@Getter
@Setter
public class FeatureAccessConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 기능 키: AI_ANALYSIS, LEADERBOARD, ACCOUNT_LINK_TOSS 등 */
    @Column(name = "feature_key", nullable = false, unique = true, length = 50)
    private String featureKey;

    /** 기능 전체 활성 여부 (false면 엔드포인트/UI 모두 차단) */
    @Column(name = "enabled", nullable = false, columnDefinition = "boolean default true")
    private boolean enabled = true;

    /** 로그인 필수 여부 (공개 라우트 중 일부 기능 선택적 요구) */
    @Column(name = "login_required", nullable = false, columnDefinition = "boolean default false")
    private boolean loginRequired = false;

    /** 어드민 표시용 설명 */
    @Column(length = 100)
    private String description;

    public FeatureAccessConfig(String featureKey, boolean enabled, boolean loginRequired, String description) {
        this.featureKey = featureKey;
        this.enabled = enabled;
        this.loginRequired = loginRequired;
        this.description = description;
    }
}
