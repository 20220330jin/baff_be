package com.sa.baff.domain;

import com.sa.baff.util.AiFeatureType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 기능 활성화/비활성화 설정
 * - 어드민에서 기능별 토글 관리 (비용 통제)
 * - RewardConfig의 enabled 패턴과 동일
 */
@Entity
@Table(name = "ai_feature_config")
@Getter
@Setter
@NoArgsConstructor
public class AiFeatureConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 기능 타입 (RUNNING / FASTING) */
    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private AiFeatureType featureType;

    /** 활성화 여부 */
    @Column(nullable = false)
    private Boolean enabled = false;

    /** 설명 */
    private String description;
}
