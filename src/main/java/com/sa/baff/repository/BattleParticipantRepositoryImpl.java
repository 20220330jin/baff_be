package com.sa.baff.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sa.baff.domain.QBattleParticipant;
import com.sa.baff.domain.QBattleRoom;
import com.sa.baff.model.dto.BattleRoomDto;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BattleParticipantRepositoryImpl extends QuerydslRepositorySupport implements BattleParticipantRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    public BattleParticipantRepositoryImpl(EntityManager entityManager) {
        super(BattleParticipantRepository.class);
        setEntityManager(entityManager);
        jpaQueryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public void leaveRoomByParticipant(Long id, String entryCode) {
        QBattleParticipant battleParticipant = QBattleParticipant.battleParticipant;
        QBattleRoom battleRoom = QBattleRoom.battleRoom;

        jpaQueryFactory.update(battleParticipant)
                .set(battleParticipant.delYn, 'Y')
                .where(battleParticipant.room.id.eq(
                        (JPAExpressions
                                .select(battleRoom.id)
                                .from(battleRoom)
                                .where(battleRoom.entryCode.eq(entryCode))
                        ))
                        .and(battleParticipant.user.id.eq(id)))
                .execute();
    }

    @Override
    public List<BattleRoomDto.getParticipantsList> getParticipantsList(String entryCode) {
        QBattleParticipant battleParticipant = QBattleParticipant.battleParticipant;
        QBattleRoom battleRoom = QBattleRoom.battleRoom;

        final BooleanExpression isDelYn = battleParticipant.delYn.eq('N');

        return jpaQueryFactory
                .select(Projections.constructor(BattleRoomDto.getParticipantsList.class,
                        battleParticipant.user.nickname,
                        battleParticipant.user.id,
                        battleParticipant.startingWeight,
                        battleParticipant.goalType,
                        battleParticipant.targetValue,
                        battleParticipant.isReady))
                .from(battleParticipant)
                .where(
                        isDelYn,
                        battleParticipant.room.id.eq(
                                JPAExpressions.select(battleRoom.id)
                                        .from(battleRoom)
                                        .where(battleRoom.entryCode.eq(entryCode))
                        )
                )
                .fetch();
    }

}
