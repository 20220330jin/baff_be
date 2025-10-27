package com.sa.baff.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

public class ReviewVO {

    @Getter
    public static class createReview {
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
        
        @JsonProperty("isWeightPrivate")
        private boolean isWeightPrivate;
        private String reviewType;
        private String battleRoomEntryCode;
        private Long goalId;
    }

    @Getter
    public static class createComment {
        private String content;
        private Long reviewId;
    }

}
