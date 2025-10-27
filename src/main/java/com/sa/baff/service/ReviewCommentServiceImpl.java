package com.sa.baff.service;

import com.sa.baff.domain.Review;
import com.sa.baff.domain.ReviewComment;
import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.ReviewDto;
import com.sa.baff.model.vo.ReviewVO;
import com.sa.baff.repository.ReviewCommentRepository;
import com.sa.baff.repository.ReviewRepository;
import com.sa.baff.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewCommentServiceImpl implements ReviewCommentService {
    private final ReviewCommentRepository reviewCommentRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Override
    public void createComment(ReviewVO.createComment createComment, String socialId) {
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N').orElseThrow(() -> new EntityNotFoundException("User not found"));

        Review review = reviewRepository.findById(createComment.getReviewId())
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));


        ReviewComment reviewComment = ReviewComment.builder()
                .review(review)
                .user(user)
                .content(createComment.getContent())
                .build();

        reviewCommentRepository.save(reviewComment);

        updateReviewCommentCount(review);
    }

    @Override
    public List<ReviewDto.getReviewCommentList> getReviewCommentList(Long reviewId, String socialId) {
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N').orElseThrow(() -> new EntityNotFoundException("User not found"));

        return reviewCommentRepository.getReviewCommentList(user, reviewId);
    }

    @Override
    public void deleteReviewComment(Long commentId, String socialId) {
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N').orElseThrow(() -> new EntityNotFoundException("User not found"));

        reviewCommentRepository.deleteReviewComment(commentId, user.getId());
    }

    private void updateReviewCommentCount(Review review) {
        Long count = reviewCommentRepository.countByReview(review);
        review.setCommentCount(count);
        reviewRepository.save(review);
    }
}
