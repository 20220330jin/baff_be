package com.sa.baff.service;

import com.sa.baff.model.dto.BattleRoomDto;
import com.sa.baff.model.vo.BattleRoomVO;

import java.util.List;

public interface BattleService {
    void createBattleRoom(BattleRoomVO.createBattleRoom createBattleRoomParam);

    List<BattleRoomDto.getBattleRoomList> getBattleRoomList(String socialId);
}
