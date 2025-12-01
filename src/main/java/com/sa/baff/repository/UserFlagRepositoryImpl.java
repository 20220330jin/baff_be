package com.sa.baff.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sa.baff.domain.QUserFlag;
import com.sa.baff.domain.UserFlag;
import com.sa.baff.model.dto.UserDto;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.time.LocalDateTime;
import java.util.List;

public class UserFlagRepositoryImpl extends QuerydslRepositorySupport implements UserFlagRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    public UserFlagRepositoryImpl(EntityManager entityManager) {
        super(UserFlag.class);
        setEntityManager(entityManager);
        jpaQueryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public List<UserDto.getUserFlagForPopUp> getUserFlagForPopUp(Long userId) {
        QUserFlag userFlag = QUserFlag.userFlag;

        return jpaQueryFactory
                .select(Projections.constructor(UserDto.getUserFlagForPopUp.class,
        userFlag.user.id,
        userFlag.flagKey,
        userFlag.regDateTime
        ))
                .from(userFlag)
                .where(userFlag.user.id.eq(userId))
                .fetch();
    }
}
