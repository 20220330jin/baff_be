package com.sa.baff.repository;

import com.sa.baff.model.dto.GoalsDto;

import java.util.List;

/**
 * 목표관리 관련 Custom Repository 모음
 */
public interface GoalsRepositoryCustom {
    /**
     * 목표 리스트 조회
     * @return
     */
    List<GoalsDto.getGoalsList> getGoalsList();
}
