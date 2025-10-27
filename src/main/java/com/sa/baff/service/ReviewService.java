package com.sa.baff.service;

import com.sa.baff.model.dto.ReviewDto;
import com.sa.baff.model.vo.ReviewVO;

import java.util.List;

public interface ReviewService {
    void createReview(ReviewVO.createReview createReviewParam, String socialId);

    ReviewDto.ReviewListResponse getReviewList(String socialId, int page, int size);

    void deleteReview(Long reviewId, String socialId);
}
