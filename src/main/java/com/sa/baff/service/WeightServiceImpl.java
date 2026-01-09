package com.sa.baff.service;

import com.sa.baff.domain.Goals;
import com.sa.baff.domain.UserB;
import com.sa.baff.domain.Weight;
import com.sa.baff.model.dto.WeightDto;
import com.sa.baff.model.vo.WeightVO;
import com.sa.baff.repository.GoalsRepository;
import com.sa.baff.repository.UserRepository;
import com.sa.baff.repository.WeightRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class WeightServiceImpl implements WeightService {

    private final WeightRepository weightRepository;
    private final UserRepository userRepository;
    private final GoalsRepository goalsRepository;

    @Override
    public void recordWeight(WeightVO.recordWeight recordWeightParam, String socialId) {
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N').orElseThrow(() -> new IllegalArgumentException("User not found"));
        // 요청 날짜를 기준으로 당일의 시작 시간과 종료 시간을 계산
        LocalDate requestDate = recordWeightParam.getRecordDate().toLocalDate();
        LocalDateTime startOfDay = requestDate.atStartOfDay();
        LocalDateTime endOfDay = requestDate.plusDays(1).atStartOfDay().minusNanos(1);

        // 해당 날짜 범위에 이미 기록이 있는지 확인
        Optional<Weight> existingWeight = weightRepository.findByUserAndRecordDateBetween(user, startOfDay, endOfDay);


        if (existingWeight.isPresent()) {
            Weight weightToUpdate = existingWeight.get();
            weightToUpdate.setWeight(recordWeightParam.getWeight());
            weightToUpdate.setRecordDate(recordWeightParam.getRecordDate());
            weightRepository.save(weightToUpdate);
        } else {
            if(user.getId() == 78L || user.getId() == 80L || user.getId() == 81L || user.getId() == 79L) {
                Goals goals = goalsRepository.findFor78(user.getId());
                if(goals.getStartWeight() == 0) {
                    goalsRepository.updateFor78(user.getId(), recordWeightParam.getWeight());
                }
            }

            // 기존 기록이 없다면, 새로운 엔티티를 생성합니다.
            Weight newWeight = Weight.builder()
                    .user(user)
                    .weight(recordWeightParam.getWeight())
                    .recordDate(recordWeightParam.getRecordDate())
                    .build();
            weightRepository.save(newWeight);
        }
    }

    @Override
    public WeightDto.getWeightList getWeightList(String socialId) {
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N').orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<Weight> weightList = weightRepository.findByUserId(user.getId());

        // 리턴할 데이터를 담을 그릇
        WeightDto.getWeightList data = new WeightDto.getWeightList();

        if (weightList.isEmpty()) {
            data.setDailyWeightRecords(List.of()); // 빈 리스트 설정
            data.setCurrentWeight(0.0);
            data.setTotalWeightChange(0.0);
            data.setRecordedDays(0);
            return data;
        }

        // 2. recordDate 기준으로 정렬된 리스트를 한 번만 만듭니다.
        List<Weight> sortedWeightList = weightList.stream()
                .sorted(Comparator.comparing(Weight::getRecordDate))
                .collect(Collectors.toList());

        double lastRecord = sortedWeightList.get(sortedWeightList.size() - 1).getWeight();
        data.setCurrentWeight(lastRecord);

        double firstRecord = sortedWeightList.get(0).getWeight();

        double totalChange = lastRecord - firstRecord;
        data.setTotalWeightChange(totalChange);

        Integer recordDays = weightList.size();
        data.setRecordedDays(recordDays);

        List<WeightDto.WeightResponseDto> responseDto = sortedWeightList.stream().map(weightData -> {
            WeightDto.WeightResponseDto resDto = new WeightDto.WeightResponseDto();
            resDto.setRecordWeight(weightData.getWeight());
            resDto.setRecordDate(weightData.getRecordDate());
            return resDto;
        }).collect(Collectors.toList());
        data.setDailyWeightRecords(responseDto);

        return data;
    }

    @Override
    public WeightDto.getCurrentWeight getCurrentWeight(String socialId) {
        // 유저 정보 조회
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N').orElseThrow(() -> new IllegalArgumentException("User not found"));
        WeightDto.getCurrentWeight weightInfo = weightRepository.getCurrentWeight(user.getId());

        return weightInfo;
    }

    @Override
    public List<WeightDto.getBattleWeightHistory> getBattleWeightHistory(WeightVO.getBattleWeightHistoryParam param) {
        List<WeightDto.getBattleWeightHistory> getBattleWeightHistory = weightRepository.getBattleWeightHistory(param);

        return getBattleWeightHistory;
    }

    @Override
    public List<WeightDto.testWeight> test() {
        List<WeightDto.testWeight> tmp = weightRepository.test();

        return tmp;
    }

    @Override
    public WeightDto.getWeightDataForDashboard getWeightDataForDashboard() {
        return weightRepository.getWeightDataForDashboard();
    }
}
