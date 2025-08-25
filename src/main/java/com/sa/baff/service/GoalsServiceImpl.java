package com.sa.baff.service;

import com.sa.baff.domain.Goals;
import com.sa.baff.model.dto.GoalsDto;
import com.sa.baff.model.vo.GoalsVO;
import com.sa.baff.repository.GoalsRepository;
import com.sa.baff.util.DateTimeUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class GoalsServiceImpl implements GoalsService {

    private final GoalsRepository goalsRepository;

    @Override
    public void recordGoals(GoalsVO.recordGoals recordGoalsParam) {
        LocalDateTime startDate = DateTimeUtils.now();
        LocalDateTime endDate = startDate;
        Goals goals = Goals.builder()
                .title(recordGoalsParam.getTitle())
                .startDate(startDate)
                .endDate(endDate.plusHours(recordGoalsParam.getPresetDuration()))
                .startWeight(recordGoalsParam.getStartWeight())
                .targetWeight(recordGoalsParam.getTargetWeight())
                .build();

        goalsRepository.save(goals);
    }

    @Override
    public List<GoalsDto.getGoalsList> getGoalsList() {
        List<Goals> goals = goalsRepository.findAll();

        List<GoalsDto.getGoalsList> goalsList = goals.stream()
                .map(goal -> {
                    GoalsDto.getGoalsList dto = new GoalsDto.getGoalsList();
                    dto.setGoalsId(goal.getId());
                    dto.setTitle(goal.getTitle());
                    dto.setStartDate(goal.getStartDate());
                    dto.setEndDate(goal.getEndDate());
                    dto.setStartWeight(goal.getStartWeight());
                    dto.setTargetWeight(goal.getTargetWeight());

                    dto.setIsExpired(goal.getEndDate().isBefore(DateTimeUtils.now()));

                    return dto;
                })
                .collect(Collectors.toList());

        return goalsList;
    }
}
