package com.sa.baff.service;

import com.sa.baff.model.dto.WeightDto;
import com.sa.baff.model.vo.WeightVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

/**
 * 체중 관리 Service 모음
 */
public interface WeightService {

    void recordWeight(WeightVO.recordWeight recordWeightParam, String socialId);

    WeightDto.getWeightList getWeightList(String socialId);

    /**
     * 현재 체중 조회 Service
     * @param socialId
     * @return
     */
    WeightDto.getCurrentWeight getCurrentWeight(String socialId);
}
