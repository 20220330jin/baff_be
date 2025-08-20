package com.sa.baff.domain;

import jakarta.persistence.*;

/**
 * 체중 관리 엔티티
 */
@Entity
public class Weight extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "weightId")
    private Long id;
}
