package com.sa.baff.model.vo;

import lombok.Getter;

import java.time.LocalDateTime;

public class RunningRecordVO {

    @Getter
    public static class RecordRunning {
        private LocalDateTime recordDate;
        private Integer durationMinutes;
        private Double distanceKm;
    }
}
