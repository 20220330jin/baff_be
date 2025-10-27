package com.sa.baff.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "review_like", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"reviewId", "userId"})
})
public class ReviewLike extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewId", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private UserB user;

    @Builder
    public ReviewLike(Review review, UserB user) {
        this.review = review;
        this.user = user;
    }

}
