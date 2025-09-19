package com.sa.baff.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sa.baff.domain.QWeight;
import com.sa.baff.domain.Weight;
import com.sa.baff.model.dto.WeightDto;
import com.sa.baff.model.vo.WeightVO;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class WeightRepositoryImpl extends QuerydslRepositorySupport implements WeightRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    public WeightRepositoryImpl(EntityManager entityManager) {
        super(Weight.class);
        setEntityManager(entityManager);
        jpaQueryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public WeightDto.getCurrentWeight getCurrentWeight(Long userId) {
        QWeight weight = QWeight.weight1;

        final BooleanExpression isDelYn = weight.delYn.eq('N');
        final BooleanExpression isUserId = weight.user.id.eq(userId);

        return jpaQueryFactory.select(Projections.constructor(WeightDto.getCurrentWeight.class,
                                    weight.weight))
                            .from(weight)
                            .where(isDelYn
                                .and(isUserId))
                            .orderBy(weight.recordDate.desc())
                            .fetchFirst();
    }

    @Override
    public List<WeightDto.getBattleWeightHistory> getBattleWeightHistory(WeightVO.getBattleWeightHistoryParam param) {
        QWeight weight = QWeight.weight1;

        final BooleanExpression isDelYn = weight.delYn.eq('N');
        final BooleanExpression isUserId = weight.user.id.eq(param.getUserId());

        return jpaQueryFactory.select(Projections.constructor(WeightDto.getBattleWeightHistory.class,
                                      weight.weight,
                                      weight.recordDate))
                            .from(weight)
                            .where(isDelYn
                                    .and(isUserId)
                                    .and(weight.recordDate.between(param.getStartDate().atStartOfDay(), param.getEndDate().atStartOfDay())))
                            .orderBy(weight.recordDate.asc())
                            .fetch();

    }
}
