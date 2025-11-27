package com.sa.baff.model.dto;

import com.sa.baff.util.GoalType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import java.time.LocalDateTime;
import java.util.List;

public class ReviewDto {

    @Getter
    public static class getReviewList {
        private Long reviewId;
        private String title;
        private String dietMethods;
        private String difficulty;
        private Double startWeight;
        private Double targetWeight;
        private Integer period;
        private String question_hardest_period;
        private String question_diet_management;
        private String question_exercise;
        private String question_effective_method;
        private String question_recommend_target;
        private String content;
        private String imageUrl1;
        private String imageUrl2;
        private boolean isWeightPrivate;
        private LocalDateTime regDateTime;
        private Long userId;
        private String userNickName;
        private String userProfileImage;
        private String reviewType;
        private String battleRoomEntryCode;
        private Long goalId;
        private Long likes;
        private Long commentCount;
        private boolean isPublic;
        private boolean isLiked;

        public getReviewList(Long reviewId, String title, String dietMethods, String difficulty,
                             Double startWeight, Double targetWeight, Integer period,
                             String question_hardest_period, String question_diet_management,
                             String question_exercise, String question_effective_method,
                             String question_recommend_target, String content,
                             String imageUrl1, String imageUrl2, boolean isWeightPrivate,
                             LocalDateTime regDateTime, Long userId, String userNickName, String userProfileImage,
                             String reviewType, String battleRoomEntryCode, Long goalId, Long likes, Long commentCount, boolean isPublic, boolean isLiked) {
            this.reviewId = reviewId;
            this.title = title;
            this.dietMethods = dietMethods;
            this.difficulty = difficulty;
            this.startWeight = startWeight;
            this.targetWeight = targetWeight;
            this.period = period;
            this.question_hardest_period = question_hardest_period;
            this.question_diet_management = question_diet_management;
            this.question_exercise = question_exercise;
            this.question_effective_method = question_effective_method;
            this.question_recommend_target = question_recommend_target;
            this.content = content;
            this.imageUrl1 = imageUrl1;
            this.imageUrl2 = imageUrl2;
            this.isWeightPrivate = isWeightPrivate;
            this.regDateTime = regDateTime;
            this.userId = userId;
            this.userNickName = userNickName;
            this.userProfileImage = userProfileImage;
            this.reviewType = reviewType;
            this.battleRoomEntryCode = battleRoomEntryCode;
            this.goalId = goalId;
            this.likes = likes;
            this.commentCount = commentCount;
            this.isPublic = isPublic;
            this.isLiked = isLiked;
        }
    }

    @Getter
    public static class ReviewListResponse {
        private List<getReviewList> content;
        private int pageNumber;
        private int pageSize;
        private long totalElements;
        private int totalPages;
        private boolean last;
        private boolean first;

        public ReviewListResponse(Page<getReviewList> page) {
            this.content = page.getContent();
            this.pageNumber = page.getNumber();
            this.pageSize = page.getSize();
            this.totalElements = page.getTotalElements();
            this.totalPages = page.getTotalPages();
            this.last = page.isLast();
            this.first = page.isFirst();
        }
    }

    @Getter
    public static class getReviewCommentList {
        private Long commentId;

        private Long userId;

        private String profileImageUrl;

        private String userNickName;

        private String content;

        private LocalDateTime regDateTime;

        public getReviewCommentList (Long commentId, Long userId, String profileImageUrl, String userNickName, String content, LocalDateTime regDateTime){
            this.commentId = commentId;
            this.userId = userId;
            this.profileImageUrl = profileImageUrl;
            this.userNickName = userNickName;
            this.content = content;
            this.regDateTime = regDateTime;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class getBattleDataForReview {
        // 작성자의 변화량, 상대 감량, 기간, 작성자의 목표량
        private Double hostWeightChange;
        private Double opponentWeightChange;
        private Long durationDays;
        private Double hostTargetWeight;
        private GoalType hostGoalType;

        public getBattleDataForReview(Double hostWeightChange, Double opponentWeightChange, Long durationDays, Double hostTargetWeight, GoalType hostGoalType) {
            this.hostWeightChange = hostWeightChange;
            this.opponentWeightChange = opponentWeightChange;
            this.durationDays = durationDays;
            this.hostTargetWeight = hostTargetWeight;
            this.hostGoalType = hostGoalType;
        }
    }
}