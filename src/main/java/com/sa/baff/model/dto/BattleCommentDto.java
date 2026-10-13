package com.sa.baff.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

public class BattleCommentDto {

    @Getter
    @AllArgsConstructor
    public static class commentItem {
        private Long commentId;
        private Long userId;
        private String userNickname;
        private String content;
        private LocalDateTime regDateTime;
        // 요청자 본인 댓글 여부 (삭제 버튼 노출용)
        private boolean mine;
    }
}
