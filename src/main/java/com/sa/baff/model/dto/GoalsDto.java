package com.sa.baff.model.dto;

import lombok.Setter;

/**
 * 목표 설정 관련 DTO 모음
 */
public class GoalsDto {

    @Setter
    public static class getGoalsList {
        /* 목표 제목 */
        private String title;
    }
}
