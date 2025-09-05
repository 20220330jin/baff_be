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
}
