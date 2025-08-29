package com.sa.baff.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 체중 목표 설정 엔티티
 *
 * @author hjkim
 */
@Entity
@Table(name = "goals")
@Getter
@NoArgsConstructor
public class Goals extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "goalsId")
    private Long id;

    private String title;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Double startWeight;
    private Double targetWeight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    private UserB user;

    @Builder
    public Goals(String title, LocalDateTime startDate, LocalDateTime endDate, Double startWeight, Double targetWeight, UserB user) {
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startWeight = startWeight;
        this.targetWeight = targetWeight;
        this.user = user;
    }
}
