package com.sa.baff.service;

import com.sa.baff.model.dto.MissionDto;
import com.sa.baff.util.MissionType;

public interface MissionService {

    /** 이번주 미션 현황 조회 */
    MissionDto.weeklyStatusResponse getWeeklyMissionStatus(String socialId);

    /** 미션 진행도 증가 (출석/체중기록 시 호출) */
    void incrementMissionProgress(Long userId, MissionType missionType);

    /** 미션 보상 수령 */
    MissionDto.claimResponse claimMissionReward(String socialId, MissionType missionType);
}
