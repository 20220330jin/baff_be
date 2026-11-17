package com.sa.baff.repository;

import com.sa.baff.domain.FastingRecord;
import com.sa.baff.domain.UserB;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FastingRecordRepository extends JpaRepository<FastingRecord, Long> {

    List<FastingRecord> findByUserAndDelYnOrderByStartTimeDesc(UserB user, Character delYn);

    /** 현재 진행중인 단식 — 종료 시각도 상태도 없는 것 (「기억 안 나요」 UNKNOWN 은 종료 시각이 없어도 진행 중이 아니다 · M-043) */
    @Query("select f from FastingRecord f where f.user = :user and f.endTime is null and f.endStatus is null and f.delYn = 'N'")
    Optional<FastingRecord> findActive(@Param("user") UserB user);

    /** 종료 처리용 잠금 조회 — 같은 기록에 동시에 온 두 번째 요청은 첫 요청이 끝날 때까지 기다린다 (M-043) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FastingRecord f where f.id = :id")
    Optional<FastingRecord> findByIdForUpdate(@Param("id") Long id);

    /** 같은 사용자의 다른 종료 기록과 [start, end) 가 겹치는가 (M-043) */
    @Query("select case when count(f) > 0 then true else false end from FastingRecord f where f.user = :user and f.id <> :excludeId and f.delYn = 'N' "
            + "and f.endTime is not null and f.startTime < :end and f.endTime > :start")
    boolean existsOverlap(@Param("user") UserB user, @Param("excludeId") Long excludeId,
                          @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    Optional<FastingRecord> findFirstByUserAndDelYnOrderByStartTimeDesc(UserB user, Character delYn);
}
