package com.sa.baff.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pieces")
@Getter
@Setter
@NoArgsConstructor
public class Piece extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserB user;

    @Column(nullable = false)
    private Long balance = 0L;

    public Piece(UserB user) {
        this.user = user;
        this.balance = 0L;
    }

    public void addBalance(Long amount) {
        this.balance += amount;
    }

    public void deductBalance(Long amount) {
        if (this.balance < amount) {
            throw new IllegalStateException("조각이 부족합니다.");
        }
        this.balance -= amount;
    }
}
