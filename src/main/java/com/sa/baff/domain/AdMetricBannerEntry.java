package com.sa.baff.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 배너(B) 광고 위치별 분해 — spec §4-1 B.
 * (metric_date, ad_position_code) UNIQUE.
 */
@Entity
@Table(name = "ad_metric_banner_entry",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"metric_date", "ad_position_code"})
        })
@NoArgsConstructor
@Getter
@Setter
public class AdMetricBannerEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    /** AdWatchLocation enum 값 또는 'OTHER' (미분리). varchar 자유 입력. */
    @Column(name = "ad_position_code", nullable = false, length = 64)
    private String adPositionCode;

    private Integer impression;

    @Column(name = "ctr_reported", precision = 5, scale = 2)
    private BigDecimal ctrReported;

    @Column(name = "ecpm_reported")
    private Integer ecpmReported;

    private Integer revenue;

    @Column(name = "actor_admin_id")
    private Long actorAdminId;
}
