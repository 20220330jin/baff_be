package com.sa.baff.api;

import com.sa.baff.domain.BattleRoom;
import com.sa.baff.model.dto.BattleRoomDto;
import com.sa.baff.model.vo.BattleRoomVO;
import com.sa.baff.service.BattleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
   배틀 모드 관련 API 모음
*/

@RestController
@RequestMapping("/api/battle")
@RequiredArgsConstructor
public class BattleRestController {
    private final BattleService battleService;

    @PostMapping("/createBattleRoom")
    public void createBattleRoom(@RequestBody BattleRoomVO.createBattleRoom createBattleRoomParam, @AuthenticationPrincipal String socialId) {
        createBattleRoomParam.setSocialId(socialId);
        battleService.createBattleRoom(createBattleRoomParam);
    }

    @GetMapping("/getBattleRoomList")
    public List<BattleRoomDto.getBattleRoomList> getBattleRoomList(@AuthenticationPrincipal String socialId) {
        return battleService.getBattleRoomList(socialId);
    }

    @PostMapping("/{entryCode}/join")
    public void joinBattleRoom(@PathVariable String entryCode, @RequestBody BattleRoomVO.joinRequest joinRequest, @AuthenticationPrincipal String socialId) {
        battleService.joinBattleRoom(entryCode, joinRequest.getPassword(), socialId);
    }

    @GetMapping("/{entryCode}/details")
    public BattleRoomDto.getBattleRoomDetails.battleRoomDetail getBattleRoomDetails(@PathVariable String entryCode) {
        return battleService.getBattleRoomDetails(entryCode);
    }

    @PostMapping("/{entryCode}/battleGoalSetting")
    public void battleGoalSetting(@PathVariable String entryCode, @RequestBody BattleRoomVO.battleGoalSetting battleGoalSetting, @AuthenticationPrincipal String socialId) {
        battleService.battleGoalSetting(entryCode, battleGoalSetting, socialId);
    }

    @PostMapping("/{entryCode}/battleStart")
    public void battleStart(@PathVariable String entryCode, @AuthenticationPrincipal String socialId) {
        battleService.battleStart(entryCode, socialId);
    }

    @GetMapping("/active")
    public BattleRoomDto.ActiveBattleData activeBattles (@AuthenticationPrincipal String socialId) {
        return battleService.activeBattles(socialId);
    }
    @GetMapping("/getEndedBattles")
    public BattleRoomDto.ActiveBattleData getEndedBattles(@AuthenticationPrincipal String socialId) {
        return battleService.getEndedBattles(socialId);
    }

    @PostMapping("/{entryCode}/deleteRoom")
    public void deleteRoom(@PathVariable String entryCode, @AuthenticationPrincipal String socialId) {
        battleService.deleteRoom(entryCode, socialId);
    }

    @PostMapping("/{entryCode}/leaveRoomByParticipant")
    public void leaveRoomByParticipant(@PathVariable String entryCode, @AuthenticationPrincipal String socialId) {
        battleService.leaveRoomByParticipant(entryCode, socialId);
    }
}
