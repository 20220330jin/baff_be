package com.sa.baff.service;

import com.sa.baff.domain.Goals;
import com.sa.baff.domain.UserB;
import com.sa.baff.domain.Weight;
import com.sa.baff.model.dto.GoalsDto;
import com.sa.baff.model.vo.GoalsVO;
import com.sa.baff.repository.GoalsRepository;
import com.sa.baff.repository.UserRepository;
import com.sa.baff.repository.WeightRepository;
import com.sa.baff.util.DateTimeUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class GoalsServiceImpl implements GoalsService {

    private final GoalsRepository goalsRepository;
    private final UserRepository userRepository;
    private final WeightRepository weightRepository;

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

        // 최종 체중 계산을 위한 현재 체중 조회
        Optional<Weight> latestWeightOpt = weightRepository.findTopByUserOrderByRecordDateDesc(user);
        Double latestLiveWeight = latestWeightOpt.map(Weight::getWeight).orElse(null);

        List<Goals> goals = goalsRepository.findByUserIdAndDelYn(user.getId(), 'N').orElse(Collections.emptyList());

        List<GoalsDto.getGoalsList> goalsList = goals.stream()
                .map(goal -> {
                    GoalsDto.getGoalsList dto = new GoalsDto.getGoalsList();
                    dto.setGoalsId(goal.getId());
                    dto.setTitle(goal.getTitle());
                    dto.setStartDate(goal.getStartDate());
                    dto.setEndDate(goal.getEndDate());
                    dto.setStartWeight(goal.getStartWeight());
                    dto.setTargetWeight(goal.getTargetWeight());

                    boolean isExpired = goal.getEndDate().isBefore(DateTimeUtils.now());
                    dto.setIsExpired(isExpired);
                    if(isExpired) {
                        weightRepository.findTopByUserAndRecordDateLessThanEqualOrderByRecordDateDesc(user, goal.getEndDate())
                                .ifPresentOrElse(weightEnd -> dto.setCurrentWeight(weightEnd.getWeight()),
                                        () -> dto.setCurrentWeight(goal.getStartWeight()));
                    } else {
                        dto.setCurrentWeight(latestLiveWeight);
                    }

                    return dto;
                })
                .collect(Collectors.toList());

        return goalsList;
    }

    @Override
    public List<GoalsDto.getGoalsList> getActiveGoalsList(String socialId) {
        // 사용자 확인
        UserB user = userRepository.findUserIdBySocialId(socialId).orElseThrow(() -> new EntityNotFoundException("User not found"));

        // 최종 체중 계산을 위한 현재 체중 조회
        Optional<Weight> latestWeightOpt = weightRepository.findTopByUserOrderByRecordDateDesc(user);
        Double latestLiveWeight = latestWeightOpt.map(Weight::getWeight).orElse(null);

        List<Goals> goals = goalsRepository.findByUserIdAndDelYn(user.getId(), 'N').orElse(Collections.emptyList());

        // 만료되지 않은 목표만 필터링하여 DTO로 변환
        List<GoalsDto.getGoalsList> goalsList = goals.stream()
                .filter(goal -> !goal.getEndDate().isBefore(LocalDateTime.now()))
                .map(goal -> {
                    GoalsDto.getGoalsList dto = new GoalsDto.getGoalsList();
                    dto.setGoalsId(goal.getId());
                    dto.setTitle(goal.getTitle());
                    dto.setStartDate(goal.getStartDate());
                    dto.setEndDate(goal.getEndDate());
                    dto.setStartWeight(goal.getStartWeight());
                    dto.setTargetWeight(goal.getTargetWeight());
                    dto.setIsExpired(false); // 위의 필터를 통해서 항상 false를 반환
                    dto.setCurrentWeight(latestLiveWeight);
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
