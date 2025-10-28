package com.sa.baff.repository;

import com.sa.baff.model.dto.ReviewDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ReviewRepositoryCustom {

    Page<ReviewDto.getReviewList> getReviewList(int page, int size, Long userId);

    void deleteReview(Long reviewId, Long userId);

    void updateReviewCommentCount(Long reviewId, Long count);
}
