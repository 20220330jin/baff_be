package com.sa.baff.repository;

import com.sa.baff.model.dto.WeightDto;

/**
 * 체중 관리 Custom Repository 모음
 */
public interface WeightRepositoryCustom {
    /**
     * 현재 체중 정보 조회 Repository
     * @param id
     * @return
     */
    WeightDto.getCurrentWeight getCurrentWeight(Long id);
}
