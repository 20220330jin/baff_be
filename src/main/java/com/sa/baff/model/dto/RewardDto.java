package com.sa.baff.model.dto;

import com.sa.baff.util.RewardType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class RewardDto {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class rewardResponse {
        private Integer earnedGrams;
        private String message;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class pointBalanceResponse {
        private Long balance;
        private Long totalEarned;
        private Long totalExchanged;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class historyResponse {
        private Long balance;
        private List<historyItem> items;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class historyItem {
        private RewardType rewardType;
        private Integer amount;
        private String description;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class exchangeRequest {
        private Integer amount;
        private Boolean adWatched;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class exchangeResponse {
        private Boolean success;
        private Integer pointAmount;
        private Integer tossAmount;
        private Long remainingBalance;
        private String message;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class adEventRequest {
        private String watchLocation;
        private Long referenceId;
        private String tossAdResponse;
    }
}
