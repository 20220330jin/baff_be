package com.sa.baff.repository;

import com.sa.baff.model.dto.ReviewDto;
import com.sa.baff.model.vo.ReviewVO;
import org.springframework.data.domain.Page;

public interface ReviewRepositoryCustom {

    Page<ReviewDto.getReviewList> getReviewList(int page, int size, Long userId, String category);

    void deleteReview(Long reviewId, Long userId);

    void updateReviewCommentCount(Long reviewId, Long count);
}
