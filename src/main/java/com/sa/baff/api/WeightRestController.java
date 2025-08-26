package com.sa.baff.api;

import com.sa.baff.model.dto.WeightDto;
import com.sa.baff.model.vo.WeightVO;
import com.sa.baff.service.WeightService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 체중 관리 관련 API 모음
 */
@RestController
@RequestMapping("/api/weight")
@RequiredArgsConstructor
public class WeightRestController {
    private final WeightService weightService;

    @PostMapping("/recordWeight")
    public void recordWeight(@RequestBody WeightVO.recordWeight recordWeightParam, @AuthenticationPrincipal String socialId) {
        weightService.recordWeight(recordWeightParam, socialId);
    }

    @GetMapping("/getWeightList")
    public WeightDto.getWeightList getWeightList(@AuthenticationPrincipal String socialId) {
        return weightService.getWeightList(socialId);
    }

    /**
     * 현재 체중 조회 API
     * - 목표 설정 페이지에서 이용
     */
    @GetMapping("/getCurrentWeight")
    public WeightDto.getCurrentWeight getCurrentWeight(@AuthenticationPrincipal String socialId) {
        return weightService.getCurrentWeight(socialId);
    }


}
