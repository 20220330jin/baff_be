package com.sa.baff.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 리뷰 엔티티
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reviewId")
    private Long id;

    private String title;

    @Column(name = "diet_methods_csv")
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

    @Column(columnDefinition = "TEXT")
    private String content;

    private String imageUrl1;

    private String imageUrl2;

    private boolean isWeightPrivate;

    private String reviewType;

    private String battleRoomEntryCode;

    private Long goalId;

    @Column(columnDefinition = "BIGINT DEFAULT 0")
    private Long likes = 0L;

    @Column(columnDefinition = "BIGINT DEFAULT 0")
    private Long commentCount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    private UserB user;

    @Builder
    public Review(String title, String dietMethods,String difficulty, Double startWeight, Double targetWeight, Integer period,String question_hardest_period, String question_diet_management, String question_exercise,
                  String question_effective_method, String question_recommend_target, String content, String imageUrl1, String imageUrl2, boolean isWeightPrivate,
                  String reviewType, String battleRoomEntryCode, Long goalId, UserB user) {
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
        this.reviewType = reviewType;
        this.battleRoomEntryCode = battleRoomEntryCode;
        this.goalId = goalId;
        this.user = user;
    }
}
