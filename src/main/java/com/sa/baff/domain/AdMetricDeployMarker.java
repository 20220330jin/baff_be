package com.sa.baff.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 배포 마커 (나만그래 deploy_marker 정합).
 * 일자별 빌드 버전 + 메모 — daily 분석 §6 배포 영향 귀인.
 * (metric_date, deploy_version) UNIQUE.
 */
@Entity
@Table(name = "ad_metric_deploy_marker",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"metric_date", "deploy_version"})
        })
@NoArgsConstructor
@Getter
@Setter
public class AdMetricDeployMarker extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(name = "deploy_version", nullable = false, length = 50)
    private String deployVersion;

    @Column(name = "deploy_note", columnDefinition = "TEXT")
    private String deployNote;

    @Column(name = "actor_admin_id")
    private Long actorAdminId;
}
