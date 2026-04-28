package com.sa.baff.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * P0 광고전략 — 일자별 토스 콘솔 reported 수치 (1행/일).
 *
 * spec v0.3 §4-3 정본.
 * - metric_date UNIQUE — 1일 1행
 * - reported 필드는 모두 nullable (부분 입력 허용, 0과 NULL 구분)
 * - R/F 노출 reported는 검증용 (truth는 DB AdWatchEvent observed)
 * - B/I 노출은 truth (DB 미수집)
 */
@Entity
@Table(name = "ad_metric_daily_entry",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "metric_date")
        })
@NoArgsConstructor
@Getter
@Setter
public class AdMetricDailyEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_date", nullable = false, unique = true)
    private LocalDate metricDate;

    // ===== 토스 콘솔 reported — 수익 =====
    @Column(name = "toss_revenue_r")
    private Integer tossRevenueR;

    @Column(name = "toss_revenue_f")
    private Integer tossRevenueF;

    @Column(name = "toss_revenue_b_total")
    private Integer tossRevenueBTotal;

    @Column(name = "toss_revenue_i")
    private Integer tossRevenueI;

    // ===== 토스 콘솔 reported — eCPM =====
    @Column(name = "ecpm_r_reported")
    private Integer ecpmRReported;

    @Column(name = "ecpm_f_reported")
    private Integer ecpmFReported;

    @Column(name = "ecpm_b_total_reported")
    private Integer ecpmBTotalReported;

    @Column(name = "ecpm_i_reported")
    private Integer ecpmIReported;

    // ===== 토스 콘솔 reported — 시청률 (%, 0~100) =====
    @Column(name = "ctr_r_reported", precision = 5, scale = 2)
    private BigDecimal ctrRReported;

    @Column(name = "ctr_f_reported", precision = 5, scale = 2)
    private BigDecimal ctrFReported;

    @Column(name = "ctr_b_total_reported", precision = 5, scale = 2)
    private BigDecimal ctrBTotalReported;

    @Column(name = "ctr_i_reported", precision = 5, scale = 2)
    private BigDecimal ctrIReported;

    // ===== 토스 콘솔 reported — 노출 =====
    /** R 노출 reported — 검증용 (truth는 DB observed) */
    @Column(name = "impression_r_reported")
    private Integer impressionRReported;

    /** F 노출 reported — 검증용 (truth는 DB observed) */
    @Column(name = "impression_f_reported")
    private Integer impressionFReported;

    /** B 노출 합산 — truth (DB 미수집) */
    @Column(name = "impression_b_total")
    private Integer impressionBTotal;

    /** I 노출 — truth (DB 미수집) */
    @Column(name = "impression_i")
    private Integer impressionI;

    // ===== 토스 콘솔 reported — 유저 (나만그래 newUsers/totalUsers 패턴) =====
    @Column(name = "new_users_reported")
    private Integer newUsersReported;

    @Column(name = "total_users_reported")
    private Integer totalUsersReported;

    @Column(name = "retention_d1_new", precision = 5, scale = 2)
    private BigDecimal retentionD1New;

    @Column(name = "retention_d1_total", precision = 5, scale = 2)
    private BigDecimal retentionD1Total;

    // ===== 토스 외 자동 미수집 =====
    @Column(name = "new_inflow_toss")
    private Integer newInflowToss;

    @Column(name = "avg_session_sec", precision = 8, scale = 2)
    private BigDecimal avgSessionSec;

    @Column(name = "benefits_tab_inflow")
    private Integer benefitsTabInflow;

    // ===== 메타 =====
    @Column(name = "actor_admin_id")
    private Long actorAdminId;
}
