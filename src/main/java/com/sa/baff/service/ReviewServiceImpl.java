package com.sa.baff.service;

import com.sa.baff.domain.Review;
import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.ReviewDto;
import com.sa.baff.model.vo.ReviewVO;
import com.sa.baff.repository.ReviewLikeRepository;
import com.sa.baff.repository.ReviewRepository;
import com.sa.baff.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService{

    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final UserRepository userRepository;

    @Override
    public void createReview(ReviewVO.createReview param, String socialId) {
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N').orElseThrow(() -> new EntityNotFoundException("User not found"));

        Review review = Review.builder()
                .title(param.getTitle())
                .dietMethods(param.getDietMethods())
                .difficulty(param.getDifficulty())
                .startWeight(param.getStartWeight())
                .targetWeight(param.getTargetWeight())
                .period(param.getPeriod())
                .question_hardest_period(param.getQuestion_hardest_period())
                .question_diet_management(param.getQuestion_diet_management())
                .question_exercise(param.getQuestion_exercise())
                .question_effective_method(param.getQuestion_effective_method())
                .question_recommend_target(param.getQuestion_recommend_target())
                .content(param.getContent())
                .imageUrl1(param.getImageUrl1())
                .imageUrl2(param.getImageUrl2())
                .isWeightPrivate(param.isWeightPrivate())
                .reviewType(param.getReviewType())
                .battleRoomEntryCode(param.getBattleRoomEntryCode())
                .goalId(param.getGoalId())
                .user(user)
                .build();

        reviewRepository.save(review);
    }

    @Override
    public ReviewDto.ReviewListResponse getReviewList(String socialId, int page, int size) {
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N').orElseThrow(() -> new EntityNotFoundException("User not found"));

        Page<ReviewDto.getReviewList> reviewPage = reviewRepository.getReviewList(page, size, user.getId());

        return new ReviewDto.ReviewListResponse(reviewPage);
    }

    @Override
    public void deleteReview(Long reviewId, String socialId) {
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N').orElseThrow(() -> new EntityNotFoundException("User not found"));

        reviewRepository.deleteReview(reviewId, user.getId());
    }
}
