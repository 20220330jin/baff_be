package com.sa.baff.api;

import com.sa.baff.model.dto.MissionDto;
import com.sa.baff.service.MissionService;
import com.sa.baff.util.MissionType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mission")
@RequiredArgsConstructor
public class MissionRestController {

    private final MissionService missionService;

    /** 이번주 미션 현황 조회 */
    @GetMapping("/weekly")
    public MissionDto.weeklyStatusResponse getWeeklyStatus(@AuthenticationPrincipal String socialId) {
        return missionService.getWeeklyMissionStatus(socialId);
    }

    /** 미션 보상 수령 */
    @PostMapping("/weekly/claim")
    public MissionDto.claimResponse claimReward(
            @AuthenticationPrincipal String socialId,
            @RequestParam String missionType) {
        return missionService.claimMissionReward(socialId, MissionType.valueOf(missionType));
    }
}
