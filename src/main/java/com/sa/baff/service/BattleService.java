package com.sa.baff.service;

import com.sa.baff.model.dto.BattleRoomDto;
import com.sa.baff.model.vo.BattleRoomVO;

import java.util.List;

public interface BattleService {
    /**
     * 대결 방을 생성하는 api
     * @param createBattleRoomParam
     */
    void createBattleRoom(BattleRoomVO.createBattleRoom createBattleRoomParam);

    /**
     * 현재 시작 대기중인 대결 방의 리스트를 반환
     * @param socialId
     * @return
     */
    List<BattleRoomDto.getBattleRoomList> getBattleRoomList(String socialId);

    /**
     * entryCode와 password를 이용하여 대결 방에 입장하는 api
     * 예외) 호스트는 방 생성 즉시 participant table에 입력 됨
     * @param entryCode
     * @param password
     * @param socialId
     */
    void joinBattleRoom(String entryCode, String password, String socialId);

    /**
     * 대결 방에 입장후 대결 대기방의 정보를 반환하는 api
     * @param entryCode
     * @return
     */
    BattleRoomDto.getBattleRoomDetails.battleRoomDetail getBattleRoomDetails(String entryCode);

    /**
     * 대결 방에 입장하여 각자의 개인 목표를 설정하는 api
     * @param entryCode
     * @param battleGoalSetting
     * @param socialId
     */
    void battleGoalSetting(String entryCode, BattleRoomVO.battleGoalSetting battleGoalSetting, String socialId);

    /**
     * 대결 방을 생성한 호스트가 참가자 모두의 준비상태를 확인 후, isReady가 true일경우 대결을 시작하는 api
     * battleStart api를 실행하면 battleRoom의 status는 WAITING에서 IN_PROGRESS로 바뀜
     * @param entryCode
     * @param socialId
     */
    void battleStart(String entryCode, String socialId);

    /**
     * 대결이 진행중인 대결 리스트를 반환하는 api
     * @param socialId
     * @return
     */
    BattleRoomDto.ActiveBattleData activeBattles(String socialId);

    /**
     * 대결이 종료된 대결 리스트를 반환하는 api
     * @param socialId
     * @return
     */
    BattleRoomDto.ActiveBattleData getEndedBattles(String socialId);

    /**
     * 방을 만든 호스트가 대결 방을 삭제하는 api
     * @param entryCode
     * @param socialId
     */
    void deleteRoom(String entryCode, String socialId);

    /**
     * 참가자가 대결 방에 입장한 후 나가기 를 했을 때 delYn = 'Y' 처리하는 api
     * @param entryCode
     * @param socialId
     */
    void leaveRoomByParticipant(String entryCode, String socialId);
}
