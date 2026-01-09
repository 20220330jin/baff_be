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

    @Getter
    @Setter
    public static class getBattleWeightHistory {
        // 체중
        private Double recordWeight;
        // 체중 기록 일자
        private LocalDateTime recordDate;

        public getBattleWeightHistory( Double recordWeight, LocalDateTime recordDate) {
            this.recordWeight = recordWeight;
            this.recordDate = recordDate;
        }
    }

    @Getter
    @Setter
    public static class testWeight {
        private Long weightId;
        private LocalDateTime regDateTime;
        private Double weight;
        private String userName;
        private Long userId;

        public testWeight(Long weightId, LocalDateTime regDateTime,
                          Double weight, String userName, Long userId) {
            this.weightId = weightId;
            this.regDateTime = regDateTime;
            this.weight = weight;
            this.userName = userName;
            this.userId = userId;
        }
    }

    @Getter
    @Setter
    public static class getWeightDataForDashboard {
        private Long weightRecordCount;
        private Double weightChangeAverage;
    }
}
