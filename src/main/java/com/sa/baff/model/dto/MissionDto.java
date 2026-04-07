package com.sa.baff.model.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

public class MissionDto {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class weeklyStatusResponse {
        private LocalDate weekStartDate;
        private LocalDate weekEndDate;
        private List<missionItem> missions;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class missionItem {
        private String missionType;
        private String title;
        private String description;
        private Integer currentCount;
        private Integer targetCount;
        private Boolean completed;
        private Boolean rewardClaimed;
        private Integer rewardAmount;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class claimResponse {
        private Integer earnedGrams;
        private String message;
    }
}
