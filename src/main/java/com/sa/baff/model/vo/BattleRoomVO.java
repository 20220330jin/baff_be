package com.sa.baff.model.vo;

import com.sa.baff.util.GoalType;
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

    @Getter
    @Setter
    public static class joinRequest {
        String password;
    }

    @Getter
    @Setter
    public static class battleGoalSetting {
        GoalType goalType;
        Double targetValue;
    }
}
