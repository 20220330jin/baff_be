package com.sa.baff.domain;

import com.sa.baff.util.AiFeatureType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AI 분석 결과 엔티티
 * - 달리기/단식 공통으로 사용
 * - Haiku/Sonnet 두 모델 결과를 함께 저장
 * - latestRecordAt vs analyzedAt 비교로 재분석 필요 여부 판단
 */
@Entity
@Table(name = "ai_analysis",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "feature_type"}))
@NoArgsConstructor
@Getter
@Setter
public class AiAnalysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 기능 타입 (RUNNING / FASTING) */
    @Enumerated(EnumType.STRING)
    @Column(name = "feature_type", nullable = false)
    private AiFeatureType featureType;

    /** Haiku 모델 분석 결과 */
    @Column(columnDefinition = "TEXT")
    private String analysisHaiku;

    /** Sonnet 모델 분석 결과 */
    @Column(columnDefinition = "TEXT")
    private String analysisSonnet;

    /** 분석 실행 시각 */
    private LocalDateTime analyzedAt;

    /** 분석에 사용된 최신 기록의 시각 (이후 새 기록이 추가되면 재분석) */
    private LocalDateTime latestRecordAt;

    @Builder
    public AiAnalysis(Long userId, AiFeatureType featureType, String analysisHaiku,
                      String analysisSonnet, LocalDateTime analyzedAt, LocalDateTime latestRecordAt) {
        this.userId = userId;
        this.featureType = featureType;
        this.analysisHaiku = analysisHaiku;
        this.analysisSonnet = analysisSonnet;
        this.analyzedAt = analyzedAt;
        this.latestRecordAt = latestRecordAt;
    }
}
