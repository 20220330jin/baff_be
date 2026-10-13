package com.sa.baff.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대결방 참여자끼리 주고받는 댓글 (스몰토크).
 * 삭제는 BaseEntity.delYn 소프트 삭제.
 */
@Entity
@Table(name = "battle_comment")
@Getter
@NoArgsConstructor
public class BattleComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roomId", nullable = false)
    private BattleRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private UserB user;

    @Column(length = 200, nullable = false)
    private String content;

    @Builder
    public BattleComment(BattleRoom room, UserB user, String content) {
        this.room = room;
        this.user = user;
        this.content = content;
    }
}
