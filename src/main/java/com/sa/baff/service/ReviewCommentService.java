package com.sa.baff.service;

import com.sa.baff.model.dto.ReviewDto;
import com.sa.baff.model.vo.ReviewVO;

import java.util.List;

public interface ReviewCommentService {

    void createComment(ReviewVO.createComment createComment, String socialId);

    List<ReviewDto.getReviewCommentList> getReviewCommentList(Long reviewId, String socialId);

    void deleteReviewComment(Long commentId, String socialId);
}
