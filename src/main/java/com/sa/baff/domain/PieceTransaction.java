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

    /** 리워드 상세 타입 (RewardConfig 참조, 배틀 트랜잭션은 null) */
    @Column(name = "reward_type")
    private String rewardType;

    /** 참조 ID (체중기록ID, 리뷰ID 등, 배틀 트랜잭션은 null) */
    @Column(name = "reference_id")
    private Long referenceId;

    @Builder
    public PieceTransaction(UserB user, Long amount, PieceTransactionType type,
                            BattleRoom battleRoom, String rewardType, Long referenceId) {
        this.user = user;
        this.amount = amount;
        this.type = type;
        this.battleRoom = battleRoom;
        this.rewardType = rewardType;
        this.referenceId = referenceId;
    }
}
