package com.sa.baff.domain;

import com.sa.baff.util.AdWatchLocation;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "ad_position_code", nullable = false, length = 64)
    private AdWatchLocation adPositionCode;

    private Integer impression;

    @Column(name = "ctr_reported", precision = 5, scale = 2)
    private BigDecimal ctrReported;

    @Column(name = "ecpm_reported")
    private Integer ecpmReported;

    private Integer revenue;

    @Column(name = "actor_admin_id")
    private Long actorAdminId;
}
