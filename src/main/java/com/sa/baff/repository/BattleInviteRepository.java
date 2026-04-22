package com.sa.baff.repository;

import com.sa.baff.domain.BattleInvite;
import com.sa.baff.domain.BattleRoom;
import com.sa.baff.domain.UserB;
import com.sa.baff.util.BattleStatus;
import com.sa.baff.util.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BattleInviteRepository extends JpaRepository<BattleInvite, Long> {

    List<BattleInvite> findAllByInviteeAndStatusAndDelYn(UserB invitee, InviteStatus status, Character delYn);

    Optional<BattleInvite> findByRoomAndInviteeAndStatusAndDelYn(BattleRoom room, UserB invitee, InviteStatus status, Character delYn);

    boolean existsByRoomAndInviteeAndStatusAndDelYn(BattleRoom room, UserB invitee, InviteStatus status, Character delYn);

    List<BattleInvite> findAllByRoomAndInviterAndDelYn(BattleRoom room, UserB inviter, Character delYn);

    /**
     * 계정 통합 차단용: 사용자가 inviter 또는 invitee인 PENDING 초대 존재 여부 (spec §3.3, §4.2).
     * S3-15 P1-6: 기존 invite 조회와 일관성 맞춰 soft-delete 제외 + 활성 방(WAITING/IN_PROGRESS) 한정.
     */
    @Query("SELECT COUNT(i) > 0 FROM BattleInvite i " +
           "WHERE (i.inviter.id = :userId OR i.invitee.id = :userId) " +
           "  AND i.status = :status " +
           "  AND i.delYn = 'N' " +
           "  AND i.room.delYn = 'N' " +
           "  AND i.room.status IN :activeStatuses")
    boolean existsPendingByUserId(@Param("userId") Long userId,
                                  @Param("status") InviteStatus status,
                                  @Param("activeStatuses") List<BattleStatus> activeStatuses);
}
