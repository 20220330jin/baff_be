package com.sa.baff.repository;

import com.sa.baff.domain.FastingRecord;
import com.sa.baff.domain.UserB;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FastingRecordRepository extends JpaRepository<FastingRecord, Long> {

    List<FastingRecord> findByUserAndDelYnOrderByStartTimeDesc(UserB user, Character delYn);

    /** 현재 진행중인 단식 (endTime이 null) */
    Optional<FastingRecord> findByUserAndEndTimeIsNullAndDelYn(UserB user, Character delYn);

    Optional<FastingRecord> findFirstByUserAndDelYnOrderByStartTimeDesc(UserB user, Character delYn);
}
