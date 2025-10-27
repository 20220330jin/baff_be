package com.sa.baff.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sa.baff.domain.QReviewComment;
import com.sa.baff.domain.ReviewComment;
import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.ReviewDto;
import com.sa.baff.util.DateTimeUtils;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class ReviewCommentRepositoryImpl extends QuerydslRepositorySupport implements ReviewCommentRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    public ReviewCommentRepositoryImpl(EntityManager entityManager) {
        super(ReviewComment.class);
        setEntityManager(entityManager);
        jpaQueryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public List<ReviewDto.getReviewCommentList> getReviewCommentList(UserB user, Long reviewId) {
        QReviewComment reviewComment = QReviewComment.reviewComment;
        BooleanExpression isDelYn = reviewComment.delYn.eq('N');

        return jpaQueryFactory
                .select(Projections.constructor(ReviewDto.getReviewCommentList.class,
                        reviewComment.id,
                        reviewComment.user.id,
                        reviewComment.user.profileImageUrl,
                        reviewComment.user.nickname,
                        reviewComment.content,
                        reviewComment.regDateTime
                        ))
                .from(reviewComment)
                .where(isDelYn
                        .and(reviewComment.review.id.eq(reviewId)))
                .orderBy(reviewComment.regDateTime.desc())
                .fetch();
    }

    @Override
    @Transactional
    public void deleteReviewComment(Long commentId, Long userId) {
        QReviewComment reviewComment = QReviewComment.reviewComment;
        BooleanExpression isUser = reviewComment.user.id.eq(userId);

        jpaQueryFactory.update(reviewComment)
                .set(reviewComment.delYn, 'Y')
                .set(reviewComment.modDateTime, DateTimeUtils.now())
                .where(reviewComment.id.eq(commentId)
                        .and(isUser))
                .execute();
    }
}
