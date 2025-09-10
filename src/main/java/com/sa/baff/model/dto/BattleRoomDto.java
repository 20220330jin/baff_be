package com.sa.baff.model.dto;

import com.sa.baff.util.BattleStatus;
import com.sa.baff.util.GoalType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

public class BattleRoomDto {

    @Setter
    @Getter
    public static class getBattleRoomList {
        // battle room name
        private String name;
        // battle room password
        private String password;
        // battle room description
        private String description;
        // battle room hostId
        private Long hostId;
        // battle room host nickName;
        private String hostNickName;
        // battle room status
        private BattleStatus status;
        // battle room maxParticipant
        private Integer maxParticipant;
        // battle room currentParticipant
        private Integer currentParticipant;
        // battle room durationDay
        private Integer durationDays;
        // battle room startDate
        private LocalDate startDate;
        // battle room endDate
        private LocalDate endDate;
        // battle room entryCode
        private String entryCode;
    }

    public static class getBattleRoomDetails {
        @Getter
        @Setter
        public static class battleRoomDetail {
            private String name;
            private String description;
            private BattleStatus status;
            private Integer maxParticipants;
            private Integer currentParticipants;
            private Integer durationDays;
            private LocalDate startDate;
            private LocalDate endDate;
            private String entryCode;
            private Long hostId;
            private List<ParticipantInfo> participants;
        }

        @Getter
        @Setter
        public static class ParticipantInfo {
            private String userNickname;
            private Long userId;
            private Double startingWeight;
            private Double currentWeight;
            private Double progress;
            private Integer rank;
            private GoalType goalType;
            private Double targetValue;
            private boolean isReady;
        }
    }

    @Builder
    @Getter
    @Setter
    public static class ActiveBattleData {
        private List<BattleSummaryData> activeBattles;
    }

    @Builder
    @Getter
    @Setter
    public static class BattleSummaryData {
        private String entryCode;
        private String opponentNickname;
        private Long opponentUserId;
        private Double myStartWeight;
        private Double opponentStartWeight;
        private Double myCurrentWeight;
        private Double opponentCurrentWeight;
        private Double myTargetWeightLoss;
        private Double opponentTargetWeightLoss;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
        private Double myWeightLoss;
        private Double opponentWeightLoss;
        private Double myProgress;
        private Double opponentProgress;
        private Integer totalDays;
        private Integer daysRemaining;
        private String winner;
    }
}
