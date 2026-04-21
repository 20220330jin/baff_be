package com.sa.baff.repository;

import com.sa.baff.domain.SmartPushHistory;
import com.sa.baff.util.SmartPushType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface SmartPushHistoryRepository extends JpaRepository<SmartPushHistory, Long> {

    Page<SmartPushHistory> findByPushTypeOrderByRegDateTimeDesc(SmartPushType pushType, Pageable pageable);

    Page<SmartPushHistory> findAllByOrderByRegDateTimeDesc(Pageable pageable);

    /** 일일 중복 방지 가드 (spec §3.5 — CP1 Round 2 §2 권장) */
    boolean existsByUserIdAndPushTypeAndRegDateTimeAfter(
            Long userId, SmartPushType pushType, LocalDateTime since);
}
