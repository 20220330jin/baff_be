package com.sa.baff.domain;

import com.sa.baff.util.FastingMode;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 간헐적 단식 기록 엔티티
 * - 타이머 시작/종료 기반 기록
 * - 모드별 목표 시간 vs 실제 단식 시간 추적
 */
@Entity
@Table(name = "fasting_record")
@NoArgsConstructor
@Getter
@Setter
public class FastingRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fasting_record_id")
    private Long id;

    /** 단식 시작 시간 */
    @Column(nullable = false)
    private LocalDateTime startTime;

    /** 단식 종료 시간 (null이면 진행중) */
    private LocalDateTime endTime;

    /** 단식 모드 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FastingMode mode;

    /** 목표 단식 시간 (시간 단위, ex: 16) */
    @Column(nullable = false)
    private Integer targetHours;

    /** 실제 단식 시간 (분 단위, 종료 시 계산) */
    private Integer actualMinutes;

    /** 달성 여부 (목표 시간 이상 단식했는지) */
    @Column(nullable = false)
    private Boolean completed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    private UserB user;

    @Builder
    public FastingRecord(LocalDateTime startTime, LocalDateTime endTime, FastingMode mode,
                         Integer targetHours, Integer actualMinutes, Boolean completed, UserB user) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.mode = mode;
        this.targetHours = targetHours;
        this.actualMinutes = actualMinutes;
        this.completed = completed != null ? completed : false;
        this.user = user;
    }
}
