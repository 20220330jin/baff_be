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
    public List<Goals> getGoalsList() {
        List<Goals> goals = goalsRepository.findAll();

        return goals;
    }
}
