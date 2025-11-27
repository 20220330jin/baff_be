package com.sa.baff.repository;

import com.sa.baff.model.dto.ReviewDto;
import com.sa.baff.model.vo.ReviewVO;

import java.util.List;

public interface ReviewCommentRepositoryCustom {

    List<ReviewDto.getReviewCommentList> getReviewCommentList(Long reviewId);

    void deleteReviewComment(Long commentId, Long userId);

    void editReviewComment(Long userId, ReviewVO.editReviewComment param);
}
