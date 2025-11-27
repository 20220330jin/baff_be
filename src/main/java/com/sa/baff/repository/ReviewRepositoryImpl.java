package com.sa.baff.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sa.baff.domain.QReview;
import com.sa.baff.domain.QReviewLike;
import com.sa.baff.domain.QUserB;
import com.sa.baff.domain.Review;
import com.sa.baff.model.dto.ReviewDto;
import com.sa.baff.model.vo.ReviewVO;
import com.sa.baff.util.DateTimeUtils;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class ReviewRepositoryImpl extends QuerydslRepositorySupport implements ReviewRepositoryCustom {

    private JPAQueryFactory jpaQueryFactory;

    public ReviewRepositoryImpl(EntityManager entityManager) {
        super(Review.class);
        setEntityManager(entityManager);
        jpaQueryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public Page<ReviewDto.getReviewList> getReviewList(int page, int size, Long userId, String category) {
        QReview review = QReview.review;
        QUserB user = QUserB.userB;
        QReviewLike reviewLike = QReviewLike.reviewLike;

        BooleanExpression isDelYn = review.delYn.eq('N');
        BooleanExpression myReviewCategory = category.equals("myReview") ? review.user.id.eq(userId) : null;
        OrderSpecifier popularCategory = category.equals("popular") ? review.likes.desc() : review.regDateTime.desc();

        JPQLQuery<Long> isLikedExistsQuery = JPAExpressions.select(reviewLike.id.count())
                .from(reviewLike)
                .where(
                        reviewLike.review.eq(review),
                        reviewLike.user.id.eq(userId)
                ).limit(1);

        // 1. 전체 개수 조회 (페이징을 위해 필요)
        Long total = jpaQueryFactory
                .select(review.count())
                .from(review)
                .where(isDelYn)
                .fetchOne();

        // null 체크
        long totalCount = (total != null) ? total : 0L;

        // 2. 페이지네이션 데이터 조회
        List<ReviewDto.getReviewList> content = jpaQueryFactory
                .select(Projections.constructor(ReviewDto.getReviewList.class,
                        review.id,
                        review.title,
                        review.dietMethods,
                        review.difficulty,
                        review.startWeight,
                        review.targetWeight,
                        review.period,
                        review.question_hardest_period,
                        review.question_diet_management,
                        review.question_exercise,
                        review.question_effective_method,
                        review.question_recommend_target,
                        review.content,
                        review.imageUrl1,
                        review.imageUrl2,
                        review.isWeightPrivate,
                        review.regDateTime,
                        review.user.id,
                        review.user.nickname,
                        review.user.profileImageUrl,
                        review.reviewType,
                        review.battleRoomEntryCode,
                        review.goalId,
                        review.likes,
                        review.commentCount,
                        review.isPublic,
                        new CaseBuilder()
                                .when(isLikedExistsQuery.exists())
                                .then(true)
                                .otherwise(false)
                ))
                .from(review)
                .join(review.user, user) // 닉네임 조회를 위해 User와 JOIN
                .where(isDelYn, myReviewCategory)
                .orderBy(popularCategory, review.regDateTime.desc())
                .offset((long) page * size)
                .limit(size)
                .fetch();

        // 3. Page 객체로 반환
        return new PageImpl<>(content, PageRequest.of(page, size), totalCount);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        QReview review = QReview.review;
        BooleanExpression isDelYn = review.delYn.eq('N');

        jpaQueryFactory.update(review)
                .set(review.delYn, 'Y')
                .set(review.modDateTime, DateTimeUtils.now())
                .where(review.id.eq(reviewId)
                        .and(review.user.id.eq(userId))
                        .and(isDelYn))
                .execute();
    }

    @Override
    @Transactional
    public void updateReviewCommentCount(Long reviewId, Long count) {
        QReview review = QReview.review;
        System.out.println("------------------" + count);
        jpaQueryFactory.update(review)
                .set(review.commentCount, count)
                .where(review.id.eq(reviewId))
                .execute();
    }
}
