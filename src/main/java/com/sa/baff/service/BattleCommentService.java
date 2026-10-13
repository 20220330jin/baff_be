package com.sa.baff.service;

import com.sa.baff.model.dto.BattleCommentDto;

import java.util.List;

public interface BattleCommentService {

    List<BattleCommentDto.commentItem> getComments(String entryCode, String socialId);

    BattleCommentDto.commentItem createComment(String entryCode, String content, String socialId);

    void deleteComment(String entryCode, Long commentId, String socialId);
}
