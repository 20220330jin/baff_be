package com.sa.baff.domain;

import com.sa.baff.util.PieceTransactionType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "piece_transactions")
@Getter
@Setter
@NoArgsConstructor
public class PieceTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserB user;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PieceTransactionType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_room_id")
    private BattleRoom battleRoom;

    @Builder
    public PieceTransaction(UserB user, Long amount, PieceTransactionType type, BattleRoom battleRoom) {
        this.user = user;
        this.amount = amount;
        this.type = type;
        this.battleRoom = battleRoom;
    }
}
