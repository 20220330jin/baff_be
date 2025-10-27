package com.sa.baff.service;

import com.sa.baff.domain.Review;
import com.sa.baff.domain.ReviewLike;
import com.sa.baff.domain.UserB;
import com.sa.baff.repository.ReviewLikeRepository;
import com.sa.baff.repository.ReviewRepository;
import com.sa.baff.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewLikeServiceImpl implements ReviewLikeService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ReviewLikeRepository reviewLikeRepository;

    /**
     * 리뷰에 대한 좋아요를 토글합니다. (누르기 / 취소)
     */
    @Override
    public boolean toggleReviewLike(Long reviewId, String socialId) {
        // 1. 사용자 및 리뷰 엔티티 조회
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N')
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        // 2. 기존 좋아요 기록 조회
        Optional<ReviewLike> existingLike = reviewLikeRepository.findByReviewAndUser(review, user);

        if (existingLike.isPresent()) {
            // 3. 이미 좋아요를 눌렀다면: 좋아요 취소 (삭제)
            reviewLikeRepository.delete(existingLike.get());

            updateReviewLikeCount(review);

            return false; // 좋아요 취소됨
        } else {
            // 4. 좋아요를 누르지 않았다면: 좋아요 생성
            ReviewLike newLike = ReviewLike.builder()
                    .review(review)
                    .user(user)
                    .build();
            reviewLikeRepository.save(newLike);

            updateReviewLikeCount(review);

            return true; // 좋아요 설정됨
        }
    }

    private void updateReviewLikeCount(Review review) {
        Long count = reviewLikeRepository.countByReview(review);
        review.setLikes(count);
        reviewRepository.save(review);
    }
}
