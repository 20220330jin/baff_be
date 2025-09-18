package com.sa.baff.repository;

import com.sa.baff.domain.BattleParticipant;
import com.sa.baff.domain.BattleRoom;
import com.sa.baff.domain.UserB;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BattleParticipantRepository extends JpaRepository<BattleParticipant, Long>, BattleParticipantRepositoryCustom {
    Optional<BattleParticipant> findByRoomAndUser(BattleRoom room, UserB user);

    /**
     * 삭제되지 않은 특정 방의 참가자 목록을 조회합니다.
     */
    List<BattleParticipant> findAllByRoomAndDelYn(BattleRoom room, char delYn);

    /**
     * 특정 방의 참가자 중 delYn == N 을 제외한 참가자를 조회합니다.
     * 본인이 아닌 다른 참가자를 조회합니다 - 대결 상대방
     */
    Optional<BattleParticipant> findByRoomAndUserNotAndDelYn(BattleRoom room, UserB user, char delYn);

    /**
     * 특정 방의 참가자 중 delYn == N 을 제외한 참가자를 조회합니다.
     */
    Optional<BattleParticipant> findByRoomAndUserAndDelYn(BattleRoom room, UserB user, char delYn);

    /**
     * 사용자가 참여한 유효한 참가자 목록 (참가 기록과 방이 모두 유효)을 조회합니다.
     */
    List<BattleParticipant> findAllByUserAndDelYnAndRoomDelYn(UserB user, char participantDelYn, char roomDelYn);

}
