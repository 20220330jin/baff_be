package com.sa.baff.api;

import com.sa.baff.model.vo.GoalsVO;
import com.sa.baff.service.GoalsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 체중 목표 설정 관련 API 모음
 */
@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalsRestController {

    private final GoalsService goalsService;

    @PostMapping("/recordGoals")
    public void recordGoals(@RequestBody GoalsVO.recordGoals recordGoalsParam){

        goalsService.recordGoals(recordGoalsParam);

    }
}
