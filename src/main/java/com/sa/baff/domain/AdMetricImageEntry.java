package com.sa.baff.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 이미지배너(I) 광고 위치별 분해 — spec §4-1 B 동일 구조.
 * (metric_date, ad_position_code) UNIQUE.
 */
@Entity
@Table(name = "ad_metric_image_entry",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"metric_date", "ad_position_code"})
        })
@NoArgsConstructor
@Getter
@Setter
public class AdMetricImageEntry extends BaseEntity {

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

    /** 해당일 매핑된 광고 ID (토스 콘솔 ID별 집계와 매칭용) */
    @Column(name = "ad_id_snapshot", length = 100)
    private String adIdSnapshot;

    @Column(name = "actor_admin_id")
    private Long actorAdminId;
}
