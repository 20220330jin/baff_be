package com.sa.baff.repository;

import com.sa.baff.domain.BattleRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BattleRoomRepository extends JpaRepository<BattleRoom, Long>, BattleRoomRepositoryCustom {
    Optional<BattleRoom> findByEntryCode(String entryCode);

    Long findRoomIdByEntryCode(String entryCode);
}
