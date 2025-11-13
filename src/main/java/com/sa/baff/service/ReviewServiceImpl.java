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
import java.util.Optional;

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
    public ReviewDto.ReviewListResponse getReviewList(String socialId, int page, int size, String category) {
        // socialId가 유효하지 않은 경우(비로그인 시)를 고려하여 Optional.empty()로 시작
        Optional<UserB> userOptional = Optional.empty();

        // socialId가 있을 때만 DB 조회를 시도
        if (socialId != null && !socialId.isEmpty()) {
            userOptional = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N');
        }

        // ⭐️ 핵심 수정 부분:
        // userOptional이 비어있으면 UserB.createNonExistingUser() (ID=0)를 반환합니다.
        UserB user = userOptional
                .orElse(UserB.createNonExistingUser());

        // user.getId()는 로그인 시 실제 ID, 비로그인 시 0L을 반환합니다.
        Long userId = user.getId();

        // Repository 메서드는 이제 userId가 0L인 경우(비로그인)를 안전하게 처리해야 합니다.
        Page<ReviewDto.getReviewList> reviewPage = reviewRepository.getReviewList(page, size, userId, category);

        return new ReviewDto.ReviewListResponse(reviewPage);
    }

    @Override
    public void deleteReview(Long reviewId, String socialId) {
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N').orElseThrow(() -> new EntityNotFoundException("User not found"));

        reviewRepository.deleteReview(reviewId, user.getId());
    }
}
