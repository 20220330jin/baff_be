package com.sa.baff.repository;

import com.sa.baff.domain.RunningRecord;
import com.sa.baff.domain.UserB;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RunningRecordRepository extends JpaRepository<RunningRecord, Long> {

    List<RunningRecord> findByUserAndDelYnOrderByRecordDateDesc(UserB user, Character delYn);

    Optional<RunningRecord> findByUserAndRecordDateBetweenAndDelYn(
            UserB user, LocalDateTime startOfDay, LocalDateTime endOfDay, Character delYn);

    Optional<RunningRecord> findFirstByUserAndDelYnOrderByRecordDateDesc(UserB user, Character delYn);
}
