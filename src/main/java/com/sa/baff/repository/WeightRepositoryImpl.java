package com.sa.baff.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sa.baff.domain.QUserB;
import com.sa.baff.domain.QWeight;
import com.sa.baff.domain.Weight;
import com.sa.baff.model.dto.WeightDto;
import com.sa.baff.model.vo.WeightVO;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                                    .and(weight.recordDate.between(param.getStartDate().atStartOfDay(), param.getEndDate().atTime(LocalTime.MAX))))
                            .orderBy(weight.recordDate.asc())
                            .fetch();

    }

    @Override
    public List<WeightDto.testWeight> test() {
        QWeight weight = QWeight.weight1;
        QUserB userB = QUserB.userB;

        return jpaQueryFactory.select(Projections.constructor(WeightDto.testWeight.class,
                weight.id,
                weight.regDateTime,
                weight.weight,
                userB.nickname,
                userB.id
                ))
                .from(weight)
                .join(userB)
                .on(weight.user.id.eq(userB.id))
                .fetch();
    }

    @Override
    public WeightDto.getWeightDataForDashboard getWeightDataForDashboard() {
        QWeight weight = QWeight.weight1;

        // 어제와 그제 날짜 계산 (SQL과 동일하게: >= start AND < end 형태)
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate dayBeforeYesterday = today.minusDays(2);

        LocalDateTime yesterdayStart = yesterday.atStartOfDay();
        LocalDateTime yesterdayEnd = today.atStartOfDay(); // 오늘 00:00:00 (어제 끝)
        LocalDateTime dayBeforeYesterdayStart = dayBeforeYesterday.atStartOfDay();
        LocalDateTime dayBeforeYesterdayEnd = yesterday.atStartOfDay(); // 어제 00:00:00 (그제 끝)

        BooleanExpression isDelYn = weight.delYn.eq('N');

        // 1. 어제 기록한 유저 수 조회
        Long weightRecordCount = jpaQueryFactory
                .select(weight.user.id.countDistinct())
                .from(weight)
                .where(isDelYn
                        .and(weight.regDateTime.goe(yesterdayStart))
                        .and(weight.regDateTime.lt(yesterdayEnd)))
                .fetchOne();

        // null 체크
        weightRecordCount = (weightRecordCount != null) ? weightRecordCount : 0L;

        // 2. 어제 각 유저별 최신 체중 조회 (userId, weight)
        List<Tuple> yesterdayWeights = jpaQueryFactory
                .select(weight.user.id, weight.weight)
                .from(weight)
                .where(isDelYn
                        .and(weight.regDateTime.goe(yesterdayStart))
                        .and(weight.regDateTime.lt(yesterdayEnd)))
                .orderBy(weight.user.id.asc(), weight.regDateTime.desc())
                .fetch();

        // 유저별로 가장 최신 체중만 추출 (첫 번째 레코드만 사용)
        Map<Long, Double> yesterdayWeightMap = yesterdayWeights.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(weight.user.id),      // userId
                        tuple -> tuple.get(weight.weight),       // weight
                        (existing, replacement) -> existing // 중복 시 첫 번째 값 유지 (이미 desc 정렬되어 있음)
                ));

        // 3. 그제 각 유저별 최신 체중 조회 (모든 유저)
        List<Tuple> dayBeforeYesterdayWeights = jpaQueryFactory
                .select(weight.user.id, weight.weight)
                .from(weight)
                .where(isDelYn
                        .and(weight.regDateTime.goe(dayBeforeYesterdayStart))
                        .and(weight.regDateTime.lt(dayBeforeYesterdayEnd)))
                .orderBy(weight.user.id.asc(), weight.regDateTime.desc())
                .fetch();

        // 유저별로 가장 최신 체중만 추출
        Map<Long, Double> dayBeforeYesterdayWeightMap = dayBeforeYesterdayWeights.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(weight.user.id),
                        tuple -> tuple.get(weight.weight),
                        (existing, replacement) -> existing
                ));

        // 4. 그제와 어제 모두 기록이 있는 유저의 체중 변화량 계산
        List<Double> weightDifferences = yesterdayWeightMap.entrySet().stream()
                .filter(entry -> dayBeforeYesterdayWeightMap.containsKey(entry.getKey())) // 그제 기록도 있는 유저만
                .map(entry -> {
                    Long userId = entry.getKey();
                    Double yesterdayWeight = entry.getValue();
                    Double dayBeforeYesterdayWeight = dayBeforeYesterdayWeightMap.get(userId);
                    return yesterdayWeight - dayBeforeYesterdayWeight; // 체중 변화량 (어제 - 그제)
                })
                .collect(Collectors.toList());

        // 5. 평균 체중 변화량 계산
        Double weightChangeAverage = 0.0;
        if (!weightDifferences.isEmpty()) {
            weightChangeAverage = weightDifferences.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
        }

        // 6. 결과 반환
        WeightDto.getWeightDataForDashboard result = new WeightDto.getWeightDataForDashboard();
        result.setWeightRecordCount(weightRecordCount);
        result.setWeightChangeAverage(weightChangeAverage);

        return result;
    }
}
