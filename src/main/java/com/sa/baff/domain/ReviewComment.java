package com.sa.baff.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "review_comment")
@Getter
@NoArgsConstructor
public class ReviewComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewId", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private UserB user;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Builder
    public ReviewComment(Review review, UserB user, String content) {
        this.review = review;
        this.user = user;
        this.content = content;
    }
}
