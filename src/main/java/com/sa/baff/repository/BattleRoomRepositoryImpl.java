package com.sa.baff.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sa.baff.domain.BattleRoom;
import com.sa.baff.domain.QBattleRoom;
import com.sa.baff.util.DateTimeUtils;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

@Repository
public class BattleRoomRepositoryImpl extends QuerydslRepositorySupport implements BattleRoomRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;


    public BattleRoomRepositoryImpl(EntityManager entityManager) {
        super(BattleRoom.class);
        setEntityManager(entityManager);
        jpaQueryFactory = new JPAQueryFactory(entityManager);


    }

    @Override
    public void deleteRoom(Long userId, String entryCode) {
        QBattleRoom battleRoom = QBattleRoom.battleRoom;

        jpaQueryFactory.update(battleRoom)
                .set(battleRoom.delYn, 'Y')
                .set(battleRoom.modDateTime, DateTimeUtils.now())
                .where(battleRoom.entryCode.eq(entryCode)
                        .and(battleRoom.host.id.eq(userId)))
                .execute();


    }
}
