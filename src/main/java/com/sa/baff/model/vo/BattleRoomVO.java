package com.sa.baff.model.vo;

import lombok.Getter;
import lombok.Setter;

public class BattleRoomVO {

    @Getter
    @Setter
    public static class createBattleRoom {
        String name;
        String password;
        String description;
        Integer maxParticipants;
        Integer durationDays;
        String socialId;
    }
}
