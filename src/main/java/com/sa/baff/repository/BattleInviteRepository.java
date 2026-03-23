package com.sa.baff.repository;

import com.sa.baff.domain.BattleInvite;
import com.sa.baff.domain.BattleRoom;
import com.sa.baff.domain.UserB;
import com.sa.baff.util.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BattleInviteRepository extends JpaRepository<BattleInvite, Long> {

    List<BattleInvite> findAllByInviteeAndStatusAndDelYn(UserB invitee, InviteStatus status, Character delYn);

    Optional<BattleInvite> findByRoomAndInviteeAndStatusAndDelYn(BattleRoom room, UserB invitee, InviteStatus status, Character delYn);

    boolean existsByRoomAndInviteeAndStatusAndDelYn(BattleRoom room, UserB invitee, InviteStatus status, Character delYn);

    List<BattleInvite> findAllByRoomAndInviterAndDelYn(BattleRoom room, UserB inviter, Character delYn);
}
