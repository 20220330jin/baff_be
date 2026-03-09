package com.sa.baff.domain;

import com.sa.baff.util.InviteStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "battle_invites")
@Getter
@Setter
@NoArgsConstructor
public class BattleInvite extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private BattleRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = false)
    private UserB inviter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_id", nullable = false)
    private UserB invitee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InviteStatus status;

    @Builder
    public BattleInvite(BattleRoom room, UserB inviter, UserB invitee) {
        this.room = room;
        this.inviter = inviter;
        this.invitee = invitee;
        this.status = InviteStatus.PENDING;
    }
}
