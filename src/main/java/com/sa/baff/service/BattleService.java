package com.sa.baff.service;

import com.sa.baff.model.dto.BattleRoomDto;
import com.sa.baff.model.vo.BattleRoomVO;

import java.util.List;

public interface BattleService {
    void createBattleRoom(BattleRoomVO.createBattleRoom createBattleRoomParam);

    List<BattleRoomDto.getBattleRoomList> getBattleRoomList(String socialId);

    void joinBattleRoom(String entryCode, String password, String socialId);

    BattleRoomDto.getBattleRoomDetails.battleRoomDetail getBattleRoomDetails(String entryCode);

    void battleGoalSetting(String entryCode, BattleRoomVO.battleGoalSetting battleGoalSetting, String socialId);

    void battleStart(String entryCode, String socialId);

    BattleRoomDto.ActiveBattleData activeBattles(String socialId);
}
