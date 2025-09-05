package com.sa.baff.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "battle_participants")
@Getter @Setter @NoArgsConstructor
public class BattleParticipant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double startingWeight;
    private Double finalWeight;
    private Integer rank;
    private boolean isReady;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserB user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private BattleRoom room;

    @Builder
    public BattleParticipant(Double startingWeight, Double finalWeight, Integer rank, UserB user, BattleRoom room, boolean isReady) {
        this.startingWeight = startingWeight;
        this.finalWeight = finalWeight;
        this.rank = rank;
        this.user = user;
        this.room = room;
        this.isReady = isReady;
    }
}
