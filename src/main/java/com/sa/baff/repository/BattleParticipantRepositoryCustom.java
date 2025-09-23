package com.sa.baff.repository;

import com.sa.baff.model.dto.BattleRoomDto;

import java.util.List;

public interface BattleParticipantRepositoryCustom {

    void leaveRoomByParticipant(Long id, String entryCode);

    List<BattleRoomDto.getParticipantsList> getParticipantsList(String entryCode);
}
