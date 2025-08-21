package com.sa.baff.model.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class GoalsVO {

    @Getter
    @Setter
    public static class recordGoals {
        /* 목표 제목 */
        String title;
        /* 목표 기간(시간) */
        Integer presetDuration;
        /* 시작 체중 */
        Integer startWeight;
        /* 목표 체중 */
        Integer targetWeight;
    }
}