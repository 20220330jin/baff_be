package com.sa.baff.service;

import com.sa.baff.model.dto.GoalsDto;
import com.sa.baff.model.dto.ReviewDto;
import com.sa.baff.model.vo.ReviewVO;

public interface ReviewService {
    void createReview(ReviewVO.createReview createReviewParam, String socialId);

    ReviewDto.ReviewListResponse getReviewList(String socialId, int page, int size, String category);

    void deleteReview(Long reviewId, String socialId);

    ReviewDto.getBattleDataForReview getBattleDataForReview(ReviewVO.getBattleDataForReview param);

    GoalsDto.getGoalDetail getGoalDataForReview(ReviewVO.getGoalDataForReview param);

    void editReview(ReviewVO.createReview param, String socialId, Long reviewId);
}
