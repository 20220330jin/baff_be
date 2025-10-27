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
import java.time.temporal.ChronoUnit;
import java.util.*;
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
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(recordGoalsParam.getSocialId(), 'N').orElseThrow(() -> new EntityNotFoundException("User not found"));

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
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N').orElseThrow(() -> new EntityNotFoundException("User not found"));

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

        // 정렬 -> 만료된 목표는 뒤로 보내기
        goalsList.sort(Comparator.comparing(GoalsDto.getGoalsList::getIsExpired));

        return goalsList;
    }

    @Override
    public List<GoalsDto.getGoalsList> getActiveGoalsList(String socialId) {
        // 사용자 확인
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId,'N').orElseThrow(() -> new EntityNotFoundException("User not found"));

        // 최종 체중 계산을 위한 현재 체중 조회
        Optional<Weight> latestWeightOpt = weightRepository.findTopByUserOrderByRecordDateDesc(user);
        Double latestLiveWeight = latestWeightOpt.map(Weight::getWeight).orElse(null);

        List<Goals> goals = goalsRepository.findByUserIdAndDelYnAndEndDateGreaterThanEqual(user.getId(), 'N', LocalDateTime.now()).orElse(Collections.emptyList());

        // 만료되지 않은 목표만 필터링하여 DTO로 변환
        List<GoalsDto.getGoalsList> goalsList = goals.stream()
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

    @Override
    public GoalsDto.getGoalDetail getGoalDetailForReview(Long goalId, String socialId) {
        // 1. 사용자 확인 및 조회
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N')
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + socialId));

        // 2. 목표(Goal) 조회 및 사용자 권한 확인
        Goals goal = goalsRepository.findByIdAndUser(goalId, user)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found or does not belong to user: " + goalId));

        // 3. 최종 체중 (현재 체중) 조회
        Optional<Weight> latestWeightOpt = weightRepository.findTopByUserOrderByRecordDateDesc(user);

        // 현재 체중 값 추출 (기록이 없으면, 안전하게 0.0 또는 목표 시작 체중 등을 기본값으로 설정할 수 있지만, 여기서는 Optional 사용)
        Double currentWeight = latestWeightOpt.map(Weight::getWeight).orElse(null);

        long durationDays = ChronoUnit.DAYS.between(
                goal.getStartDate().toLocalDate(), // 목표 시작일
                goal.getEndDate().toLocalDate()    // 목표 종료일
        ); // 종료일을 포함하기 위해 +1 (예: 1일부터 3일까지는 3일)

        // 4. DTO 객체 생성 및 매핑
        GoalsDto.getGoalDetail dto = new GoalsDto.getGoalDetail();

        // 목표 정보 매핑
        dto.setGoalsId(goal.getId());
        dto.setDurationDays(durationDays);
        dto.setStartWeight(goal.getStartWeight()); // 목표 시작 시의 체중
        dto.setTargetWeight(goal.getTargetWeight()); // 목표 체중
        weightRepository.findTopByUserAndRecordDateLessThanEqualOrderByRecordDateDesc(user, goal.getEndDate())
                    .ifPresentOrElse(weightEnd -> dto.setCurrentWeight(weightEnd.getWeight()),
                            () -> dto.setCurrentWeight(goal.getStartWeight()));

        return dto;
    }
}
