package com.sa.baff.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 체중 관리 엔티티
 */
@Entity
@NoArgsConstructor
@Getter
public class Weight extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "weightId")
    private Long id;

    private Double weight;
    private LocalDateTime recordDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    private UserB user;

    @Builder
    public Weight(Double weight, LocalDateTime recordDate, UserB user) {
        this.weight = weight;
        this.recordDate = recordDate;
        this.user = user;
    }
}
