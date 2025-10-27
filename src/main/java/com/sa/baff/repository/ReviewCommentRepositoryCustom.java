package com.sa.baff.repository;

import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.ReviewDto;

import java.util.List;

public interface ReviewCommentRepositoryCustom {

    List<ReviewDto.getReviewCommentList> getReviewCommentList(UserB user, Long reviewId);

    void deleteReviewComment(Long commentId, Long userId);
}
