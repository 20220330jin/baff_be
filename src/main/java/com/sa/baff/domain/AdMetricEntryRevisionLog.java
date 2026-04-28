package com.sa.baff.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.Map;

/**
 * P0 광고전략 — D+7 이후 수정 시에만 적재되는 revision log.
 *
 * spec v0.3 §4-3 정본.
 * - D+7 미만 수정 → 일반 updated_at 갱신만, 본 row 미적재
 * - D+7 이후 수정 → row 1건 적재 + reason NOT NULL 강제
 * - 기준일: 수정 시점 now() 기준 아니라 row.metricDate 기준 D+7
 * - diff: changed columns only ({"changed": {col: {"before": x, "after": y}}})
 */
@Entity
@Table(name = "ad_metric_entry_revision_log",
        indexes = {
                @Index(name = "idx_ad_metric_revision_log_lookup",
                        columnList = "table_name, row_metric_date")
        })
@NoArgsConstructor
@Getter
@Setter
public class AdMetricEntryRevisionLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 'AdMetricDailyEntry' | 'AdMetricBannerEntry' (P1) */
    @Column(name = "table_name", nullable = false, length = 64)
    private String tableName;

    @Column(name = "row_metric_date", nullable = false)
    private LocalDate rowMetricDate;

    /** BannerEntry 전용 (P1). DailyEntry는 null. */
    @Column(name = "row_ad_position_code", length = 64)
    private String rowAdPositionCode;

    /** changed columns only — {"changed": {"toss_revenue_r": {"before": 350, "after": 380}}} */
    @Column(name = "diff", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> diff;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion = 1;

    /** D+7 이후 수정 사유 — NOT NULL */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "actor_admin_id", nullable = false)
    private Long actorAdminId;
}
