package com.sa.baff.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sa.baff.domain.EditNicknameStatus;
import com.sa.baff.domain.QUserB;
import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.UserBDto;
import com.sa.baff.model.dto.UserDto;
import com.sa.baff.model.vo.UserVO;
import com.sa.baff.util.DateTimeUtils;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public void editProfileImage(Long userId, UserVO.editProfileImage param) {
        QUserB user = QUserB.userB;

        jpaQueryFactory.update(user)
                .set(user.profileImageUrl, param.getImageUrl())
                .set(user.modDateTime, DateTimeUtils.now())
                .where(user.delYn.eq('N')
                        .and(user.id.eq(userId)))
                .execute();
    }

    @Override
    @Transactional
    public UserDto.editNicknameStatus editNickname(Long userId, String nickname) {
        QUserB userEntity = QUserB.userB;

        // 닉네임 중복 체크
        Long duplicateCount = jpaQueryFactory
                .select(userEntity.count())
                .from(userEntity)
                .where(userEntity.nickname.eq(nickname)
                        .and(userEntity.id.ne(userId))
                        .and(userEntity.delYn.eq('N'))
                )
                .fetchOne();

        // 이미 존재하는 닉네임으로 수정하는 경우 DUPLICATE 반환
        if(duplicateCount != null & duplicateCount > 0) {
            UserDto.editNicknameStatus response = new UserDto.editNicknameStatus();
            response.setStatus(EditNicknameStatus.DUPLICATE);
            return response;
        }

        // 존재하는 닉네임이 아니라면 닉네임 수정
        jpaQueryFactory.update(userEntity)
                .set(userEntity.nickname, nickname)
                .set(userEntity.modDateTime, DateTimeUtils.now())
                .where(userEntity.id.eq(userId))
                .execute();

        UserDto.editNicknameStatus response = new UserDto.editNicknameStatus();
        response.setStatus(EditNicknameStatus.SUCCESS);

        return response;
    }

}
