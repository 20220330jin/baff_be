package com.sa.baff.model.dto;

import com.sa.baff.util.FastingEndStatus;
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
        /** 시작 후 48시간이 지나 「종료 시각 확인 필요」 (M-043 · 조회 때 계산) */
        private Boolean needsEndTime;
        /** 진행 상한 시간 (M-043) */
        private Integer maxHours;
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
        /** 실제 지급 그램 (M-043 · 한도 초과 · 이미 지급 · 미확인이면 0) */
        private Integer earnedGrams;
        private FastingEndStatus endStatus;

        public EndFastingResponse(Long id, Integer actualMinutes, Boolean completed,
                                  Integer earnedGrams, FastingEndStatus endStatus) {
            this.id = id;
            this.actualMinutes = actualMinutes;
            this.completed = completed;
            this.earnedGrams = earnedGrams;
            this.endStatus = endStatus;
        }
    }
}
