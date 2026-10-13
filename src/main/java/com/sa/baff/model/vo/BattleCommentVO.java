package com.sa.baff.model.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class BattleCommentVO {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class createComment {
        private String content;
    }
}
