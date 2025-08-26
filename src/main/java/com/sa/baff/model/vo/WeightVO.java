package com.sa.baff.model.vo;

import lombok.Getter;

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

}
