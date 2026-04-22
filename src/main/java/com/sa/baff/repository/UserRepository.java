package com.sa.baff.repository;

import com.sa.baff.domain.UserB;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends CrudRepository<UserB, Long>, UserRepositoryCustom {
    // socialId를 통해 UserB 조회하는 메소드를 추가합니다.
    boolean existsBySocialIdAndDelYn(String socialId, Character delYn);

    Optional<UserB> findUserIdBySocialId(String socialId);

    Optional<UserB> findUserIdBySocialIdAndDelYn(String socialId, Character delYn);

    List<UserB> findAllByIdIn(List<Long> ids);

    Optional<UserB> findUserIdById(Long id);

    Optional<UserB> findBySocialId(String socialId);

    Iterable<UserB> findAllByOrderByRegDateTimeDesc();

    long countBynicknameStartingWith(String prefix);

    List<UserB> findByNicknameContainingAndDelYn(String nickname, Character delYn);

    /**
     * 계정 통합 Primary 탈퇴 시 연쇄 탈퇴 대상 Secondary 조회.
     * S3-15 P1-3: 병합된 Secondary(MERGED)는 primary_user_id로 역참조 가능.
     */
    Optional<UserB> findByPrimaryUserId(Long primaryUserId);

    /**
     * 계정 통합 confirmLink 트랜잭션 전용 비관적 락.
     * S3-15 P1-2: primary/secondary 동시 수정 경합(배틀 참가/초대 생성) 방지.
     * 호출자는 반드시 ID 정렬 순서대로 획득해서 deadlock을 방지해야 한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserB u WHERE u.id = :id")
    Optional<UserB> findByIdForUpdate(@Param("id") Long id);
}
