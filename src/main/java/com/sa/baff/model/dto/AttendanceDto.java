package com.sa.baff.model.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

public class AttendanceDto {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class checkResponse {
        private Integer earnedGrams;
        private Integer streakCount;
        private Boolean streakBonusEarned;
        private Integer streakBonusGrams;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class statusResponse {
        private Boolean attendedToday;
        private Integer currentStreak;
        private List<LocalDate> monthAttendance;
        private List<streakBonusInfo> nextBonuses;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class streakBonusInfo {
        private Integer requiredDays;
        private Integer bonusGrams;
        private Boolean achieved;
    }
}
