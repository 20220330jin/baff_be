package com.sa.baff.service;

import com.sa.baff.domain.UserB;
import com.sa.baff.domain.Weight;
import com.sa.baff.model.dto.WeightDto;
import com.sa.baff.model.vo.WeightVO;
import com.sa.baff.repository.UserRepository;
import com.sa.baff.repository.WeightRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class WeightServiceImpl implements WeightService {

    private final WeightRepository weightRepository;
    private final UserRepository userRepository;

    @Override
    public void recordWeight(WeightVO.recordWeight recordWeightParam, String socialId) {
        UserB user = userRepository.findUserIdBySocialId(socialId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        Weight weight = Weight.builder()
                .user(user)
                .weight(recordWeightParam.getWeight())
                .recordDate(recordWeightParam.getRecordDate())
                .build();

        weightRepository.save(weight);
    }

    @Override
    public WeightDto.getWeightList getWeightList(String socialId) {
        // 가장 최신의 체중
        // 총 변화량
        // 기록된 일수
        UserB user = userRepository.findUserIdBySocialId(socialId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<Weight> weightList = weightRepository.findByUserId(user.getId());

        // 리턴할 데이터를 담을 그릇
        WeightDto.getWeightList data = new WeightDto.getWeightList();
        // weightDto 에 정의해둔 WeightResponse - recordDate, recordWeight
        List<WeightDto.WeightResponseDto> responseDto = weightList.stream().map(weightData -> {
            WeightDto.WeightResponseDto resDto = new WeightDto.WeightResponseDto();
            resDto.setRecordWeight(weightData.getWeight());
            resDto.setRecordDate(weightData.getRecordDate());
            return resDto;
        }).collect(Collectors.toList());
        data.setDailyWeightRecords(responseDto);

        double lastRecord = weightList.stream()
                .sorted(Comparator.comparing(Weight::getRecordDate).reversed())
                .collect(Collectors.toList()).get(0).getWeight();
        data.setCurrentWeight(lastRecord);

        double firstRecord = weightList.stream()
                .sorted(Comparator.comparing(Weight::getRecordDate).reversed())
                .collect(Collectors.toList()).get(weightList.size() - 1).getWeight();
        data.setCurrentWeight(lastRecord);

        double totalChange = lastRecord - firstRecord;
        data.setTotalWeightChange(totalChange);

        Integer recordDays = weightList.size();
        data.setRecordedDays(recordDays);

        return data;
    }
}
