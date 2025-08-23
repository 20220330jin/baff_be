package com.sa.baff.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sa.baff.domain.Goals;
import com.sa.baff.model.dto.GoalsDto;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class GoalsRepositoryImpl extends QuerydslRepositorySupport implements GoalsRepositoryCustom {

    private JPAQueryFactory jpaQueryFactory;

    public GoalsRepositoryImpl(EntityManager entityManager) {
        super(Goals.class);
        setEntityManager(entityManager);
        jpaQueryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public List<GoalsDto.getGoalsList> getGoalsList() {
        return List.of();
    }
}
