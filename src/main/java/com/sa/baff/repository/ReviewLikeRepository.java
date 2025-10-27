package com.sa.baff.repository;

import com.sa.baff.domain.Review;
import com.sa.baff.domain.ReviewLike;
import com.sa.baff.domain.UserB;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 리뷰 좋아요 Repository
 */
public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {

    /**
     * 특정 리뷰에 대한 특정 사용자의 좋아요 기록을 조회합니다.
     * @param review 좋아요 대상 리뷰
     * @param user 좋아요를 누른 사용자
     * @return 좋아요 기록 Optional
     */
    Optional<ReviewLike> findByReviewAndUser(Review review, UserB user);

    /**
     * 특정 리뷰의 좋아요 개수를 집계합니다.
     * @param review 좋아요 대상 리뷰
     * @return 좋아요 개수
     */
    long countByReview(Review review);

    List<ReviewLike> findAllByReviewIdInAndUser(List<Long> reviewIds, UserB user);
}