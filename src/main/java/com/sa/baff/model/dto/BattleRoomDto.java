package com.sa.baff.model.dto;

import com.sa.baff.util.BattleStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

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
        private String hostId;
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

}
