package com.sa.baff.service;

import com.sa.baff.model.vo.GoalsVO;

/**
 * 목표설정 관련 서비스 모음
 */
public interface GoalsService {
    /**
     * 목표설정 저장 서비스
     * @param recordGoalsParam
     */
    void recordGoals(GoalsVO.recordGoals recordGoalsParam);
}
