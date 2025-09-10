package com.sa.baff.repository;

import com.sa.baff.domain.BattleParticipant;
import com.sa.baff.domain.BattleRoom;
import com.sa.baff.domain.UserB;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BattleParticipantRepository extends JpaRepository<BattleParticipant, Long> {
     // 방의 특정 유저 참가자 조회
    Optional<BattleParticipant> findByRoomAndUser(BattleRoom room, UserB user);

     // 방의 모든 참가자 목록 조회
    List<BattleParticipant> findAllByRoom(BattleRoom room);

     // 유저가 참여한 모든 배틀 참가자 목록 조회
    List<BattleParticipant> findAllByUser(UserB user);

    // 방에 사용자가 아닌 다른 참가자를 찾는다.
    Optional<BattleParticipant> findByRoomAndUserNot(BattleRoom room, UserB user);
}
