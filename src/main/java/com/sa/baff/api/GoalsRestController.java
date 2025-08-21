package com.sa.baff.api;

import com.sa.baff.domain.Goals;
import com.sa.baff.model.dto.GoalsDto;
import com.sa.baff.model.vo.GoalsVO;
import com.sa.baff.service.GoalsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 체중 목표 설정 관련 API 모음
 */
@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GoalsRestController {

    private final GoalsService goalsService;

    @PostMapping("/recordGoals")
    public void recordGoals(@RequestBody GoalsVO.recordGoals recordGoalsParam){
        goalsService.recordGoals(recordGoalsParam);
    }

    @GetMapping("/getGoalsList")
    public List<Goals> getGoalsList() {
        return goalsService.getGoalsList();
    }
}
