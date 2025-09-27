package com.sa.baff.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sa.baff.domain.QUserB;
import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.UserBDto;
import com.sa.baff.util.DateTimeUtils;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class UserRepositoryImpl extends QuerydslRepositorySupport implements UserRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    public UserRepositoryImpl(EntityManager entityManager) {
        super(UserB.class);
        setEntityManager(entityManager);
        jpaQueryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public UserBDto.getUserInfo getUserInfoForProfile(Long userId) {
        QUserB user = QUserB.userB;

        final BooleanExpression isDelYn = user.delYn.eq('N');
        final BooleanExpression isUserId = user.id.eq(userId);

        return jpaQueryFactory.select(Projections.constructor(UserBDto.getUserInfo.class,
                                    user.id,
                                    user.nickname,
                                    user.email,
                                    user.profileImageUrl,
                                    user.regDateTime,
                                    user.provider))
                                .from(user)
                                .where(isUserId
                                    .and(isDelYn))
                                .fetchOne();
    }

    @Override
    public void withdrawal(Long userId) {
        QUserB user = QUserB.userB;

        final BooleanExpression isDelYn = user.delYn.eq('N');
        final BooleanExpression isUserId = user.id.eq(userId);

        String uuid = UUID.randomUUID().toString();
        String withdrawalPrefix = "withdrawalUser_";

        jpaQueryFactory.update(user)
                .set(user.socialId, user.socialId.prepend(withdrawalPrefix).concat("_").concat(uuid))
                .set(user.email, user.email.prepend(withdrawalPrefix).concat(uuid))
                .set(user.delYn, 'Y')
                .set(user.modDateTime, DateTimeUtils.now())
                .where(isDelYn
                        .and(isUserId))
                .execute();
    }
}
