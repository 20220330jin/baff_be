package com.sa.baff.repository;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sa.baff.domain.QBattleParticipant;
import com.sa.baff.domain.QBattleRoom;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

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

}
