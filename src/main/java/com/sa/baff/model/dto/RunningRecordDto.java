package com.sa.baff.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class RunningRecordDto {

    @Getter
    @Setter
    public static class RunningRecordResponse {
        private Long id;
        private LocalDateTime recordDate;
        private Integer durationMinutes;
        private Double distanceKm;
        private Double paceMinPerKm; // 계산: durationMinutes / distanceKm
    }

    @Getter
    @Setter
    public static class GetRunningList {
        private Integer totalRecords;
        private Double totalDistanceKm;
        private Integer totalDurationMinutes;
        private List<RunningRecordResponse> records;
    }

    @Getter
    @Setter
    public static class RecordRunningResponse {
        private Long id;

        public RecordRunningResponse(Long id) {
            this.id = id;
        }
    }
}
