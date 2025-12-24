package com.sa.baff.repository;

import com.sa.baff.domain.Goals;
import com.sa.baff.model.dto.GoalsDto;

import java.util.List;
import java.util.Optional;

/**
 * 목표관리 관련 Custom Repository 모음
 */
public interface GoalsRepositoryCustom {
    /**
     * 목표 리스트 조회
     * @return
     */
    List<GoalsDto.getGoalsList> getGoalsList();

    /**
     * 목표 삭제
     * @param goalId
     */
    void deleteGoals(Long goalId);

    Goals findFor78(long l);

    void updateFor78(long l, Double w);
}
