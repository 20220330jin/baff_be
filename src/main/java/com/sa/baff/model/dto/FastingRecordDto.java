package com.sa.baff.model.dto;

import com.sa.baff.util.FastingMode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class FastingRecordDto {

    @Getter
    @Setter
    public static class FastingRecordResponse {
        private Long id;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private FastingMode mode;
        private Integer targetHours;
        private Integer actualMinutes;
        private Boolean completed;
    }

    @Getter
    @Setter
    public static class GetFastingList {
        private Integer totalRecords;
        private Integer completedRecords;
        private Integer totalFastingMinutes;
        private Double averageFastingMinutes;
        private Integer currentStreak; // 연속 달성 일수
        private List<FastingRecordResponse> records;
    }

    /** 현재 진행중인 단식 상태 */
    @Getter
    @Setter
    public static class ActiveFasting {
        private Long id;
        private LocalDateTime startTime;
        private FastingMode mode;
        private Integer targetHours;
        private Long elapsedMinutes; // 현재까지 경과 시간(분)
    }

    @Getter
    @Setter
    public static class StartFastingResponse {
        private Long id;

        public StartFastingResponse(Long id) {
            this.id = id;
        }
    }

    @Getter
    @Setter
    public static class EndFastingResponse {
        private Long id;
        private Integer actualMinutes;
        private Boolean completed;

        public EndFastingResponse(Long id, Integer actualMinutes, Boolean completed) {
            this.id = id;
            this.actualMinutes = actualMinutes;
            this.completed = completed;
        }
    }
}
