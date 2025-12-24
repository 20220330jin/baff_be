package com.sa.baff.repository;

import com.sa.baff.model.dto.WeightDto;
import com.sa.baff.model.vo.WeightVO;

import java.util.List;

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

    /**
     * 진행중인 배틀의 참가한 유저의 체중 기록 조회
     * @param param
     * @return
     */
    List<WeightDto.getBattleWeightHistory> getBattleWeightHistory(WeightVO.getBattleWeightHistoryParam param);

    List<WeightDto.testWeight> test();
}
