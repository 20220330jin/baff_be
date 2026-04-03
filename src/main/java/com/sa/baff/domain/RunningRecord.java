package com.sa.baff.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 달리기 기록 엔티티
 * - Weight 엔티티와 동일한 패턴
 * - 날짜(과거 가능) + 시간(분) + 거리(km) 기록
 */
@Entity
@Table(name = "running_record")
@NoArgsConstructor
@Getter
@Setter
public class RunningRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "running_record_id")
    private Long id;

    /** 기록 날짜 (과거 날짜 입력 가능) */
    private LocalDateTime recordDate;

    /** 달리기 시간 (분 단위) */
    @Column(nullable = false)
    private Integer durationMinutes;

    /** 달리기 거리 (km, 소수점 허용) */
    @Column(nullable = false)
    private Double distanceKm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    private UserB user;

    @Builder
    public RunningRecord(LocalDateTime recordDate, Integer durationMinutes, Double distanceKm, UserB user) {
        this.recordDate = recordDate;
        this.durationMinutes = durationMinutes;
        this.distanceKm = distanceKm;
        this.user = user;
    }
}
