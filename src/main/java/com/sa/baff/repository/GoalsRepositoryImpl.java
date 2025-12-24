package com.sa.baff.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sa.baff.domain.Goals;
import com.sa.baff.domain.QGoals;
import com.sa.baff.model.dto.GoalsDto;
import com.sa.baff.util.DateTimeUtils;
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

    @Override
    public void deleteGoals(Long goalId) {
        QGoals goals = QGoals.goals;

        jpaQueryFactory.update(goals)
                .set(goals.delYn, 'Y')
                .set(goals.modDateTime, DateTimeUtils.now())
                .where(goals.id.eq(goalId))
                .execute();
    }

    @Override
    public Goals findFor78(long l) {
        QGoals goals = QGoals.goals;
        return jpaQueryFactory.select(
                goals
        )
                .from(goals)
                .where(goals.user.id.eq(l))
                .fetchFirst();
    }

    @Override
    public void updateFor78(long l, Double d) {
        QGoals goals = QGoals.goals;

        jpaQueryFactory.update(goals)
                .set(goals.startWeight, d)
                .where(goals.user.id.eq(l))
                .execute();
    }
}
