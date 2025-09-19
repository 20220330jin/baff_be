package com.sa.baff.model.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 체중 관리 VO 모음
 */
public class WeightVO {

    @Getter
    public static class recordWeight {
        private Double weight;
        private LocalDateTime recordDate;
    }

    @Getter
    @Setter
    public static class getBattleWeightHistoryParam {
        private Long userId;
        private LocalDate startDate;
        private LocalDate endDate;
    }

}
