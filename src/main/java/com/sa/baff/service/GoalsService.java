package com.sa.baff.service;

import com.sa.baff.domain.Goals;
import com.sa.baff.model.dto.GoalsDto;
import com.sa.baff.model.vo.GoalsVO;

import java.util.List;

/**
 * 목표설정 관련 서비스 모음
 */
public interface GoalsService {
    /**
     * 목표설정 저장 서비스
     * @param recordGoalsParam
     */
    void recordGoals(GoalsVO.recordGoals recordGoalsParam);

    /**
     * 저장 목표 리스트 조회 서비스
     */
    List<GoalsDto.getGoalsList> getGoalsList(String socialId);
}
