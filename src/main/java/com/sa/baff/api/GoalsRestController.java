package com.sa.baff.api;

import com.sa.baff.domain.Goals;
import com.sa.baff.model.dto.GoalsDto;
import com.sa.baff.model.vo.GoalsVO;
import com.sa.baff.service.GoalsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 체중 목표 설정 관련 API 모음
 */
@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalsRestController {

    private final GoalsService goalsService;

    @PostMapping("/recordGoals")
    public void recordGoals(@RequestBody GoalsVO.recordGoals recordGoalsParam, @AuthenticationPrincipal String socialId){
        recordGoalsParam.setSocialId(socialId);
        goalsService.recordGoals(recordGoalsParam);
    }

    @GetMapping("/getGoalsList")
    public List<GoalsDto.getGoalsList> getGoalsList(@AuthenticationPrincipal String socialId) {
        return goalsService.getGoalsList(socialId);
    }

    /**
     * 진행중인 목표 리스트만 조회하는 API
     */
    @GetMapping("/getActiveGoalsList")
    public List<GoalsDto.getGoalsList> getActiveGoalsList(@AuthenticationPrincipal String socialId) {
        return goalsService.getActiveGoalsList(socialId);
    }

    /**
     * 목표 삭제 api
     * @param goalId
     */
    @PostMapping("/deleteGoal/{goalId}")
    public void deleteGoal(@PathVariable Long goalId) {
        goalsService.deleteGoal(goalId);
    }
}
