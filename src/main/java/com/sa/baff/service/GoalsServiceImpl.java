package com.sa.baff.service;

import com.sa.baff.domain.Goals;
import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.GoalsDto;
import com.sa.baff.model.vo.GoalsVO;
import com.sa.baff.repository.GoalsRepository;
import com.sa.baff.repository.UserRepository;
import com.sa.baff.util.DateTimeUtils;
import jakarta.persistence.EntityNotFoundException;
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
    private final UserRepository userRepository;

    @Override
    public void recordGoals(GoalsVO.recordGoals recordGoalsParam) {
        // 유저 정보 조회
        UserB user = userRepository.findUserIdBySocialId(recordGoalsParam.getSocialId()).orElseThrow(() -> new EntityNotFoundException("User not found"));

        LocalDateTime startDate = DateTimeUtils.now();
        LocalDateTime endDate = startDate;
        Goals goals = Goals.builder()
                .title(recordGoalsParam.getTitle())
                .startDate(startDate)
                .endDate(endDate.plusHours(recordGoalsParam.getPresetDuration()))
                .startWeight(recordGoalsParam.getStartWeight())
                .targetWeight(recordGoalsParam.getTargetWeight())
                .user(user)
                .build();

        goalsRepository.save(goals);
    }

    @Override
    public List<GoalsDto.getGoalsList> getGoalsList(String socialId) {
        // 사용자 확인
        UserB user = userRepository.findUserIdBySocialId(socialId).orElseThrow(() -> new EntityNotFoundException("User not found"));

        List<Goals> goals = goalsRepository.findByUserIdAndDelYn(user.getId(), 'N').orElse(null);

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

    @Override
    public void deleteGoal(Long goalId) {
        goalsRepository.deleteGoals(goalId);
    }
}
