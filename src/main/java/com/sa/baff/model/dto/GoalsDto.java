package com.sa.baff.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 목표 설정 관련 DTO 모음
 */
public class GoalsDto {

    @Setter
    @Getter
    public static class getGoalsList {
        /* 목표 ID */
        private Long goalsId;
        /* 목표 제목 */
        private String title;
        /* 목표 기간(시작일) */
        private LocalDateTime startDate;
        /* 목표 기간(종료일) */
        private LocalDateTime endDate;
        /* 시작 몸무게 */
        private Double startWeight;
        /* 목표 몸무게 */
        private Double targetWeight;
        /* 목표기간 만료여부 */
        private Boolean isExpired;
//        /* 만료시 최종 체중 */
//        private Double finalWeight;
        /**/
        private Double currentWeight;

    }

    @Setter
    @Getter
    public static class getGoalDetail {
        /* 목표 ID */
        private Long goalsId;
//        /* 목표 기간(시작일) */
//        private LocalDateTime startDate;
//        /* 목표 기간(종료일) */
//        private LocalDateTime endDate;
        /* 목표 기간 (일수) */
        private Long durationDays;
        /* 시작 몸무게 */
        private Double startWeight;
        /* 목표 몸무게 */
        private Double targetWeight;
        /* 현재 몸무게 */
        private Double currentWeight;
    }
}
