package com.sa.baff.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 체중 관리 DTO 모음
 */
public class WeightDto {

    @Setter
    @Getter
    // 현재 체중, 총 변화량, 기록된 일수, 날짜별 체중 기록
    public static class getWeightList {
        private Double currentWeight; // 가장 최신 체중
        private Double totalWeightChange; // 총 변화량 (초기 대비)
        private Integer recordedDays; // 기록된 일수
        private List<WeightResponseDto> dailyWeightRecords; // 날짜별 체중 기록 리스트
    }

    // 전체 체중 기록 응답 DTO
    @Getter
    @Setter
    public static class WeightResponseDto {
        // 체중 기록 일자
        private LocalDateTime recordDate;
        // 체중
        private Double recordWeight;
        // 전일대비 변화량은 프론트에서 처리
    }

    /**
     * 현재 체중 조회 DTO
     */
    @Setter
    @Getter
    public static class getCurrentWeight {
        /* 현재 체중 */
        private Double currentWeight;

        public getCurrentWeight(Double currentWeight) {
            this.currentWeight = currentWeight;
        }
    }
}
