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

    /**
     * 일일 중복 방지 가드 (spec §3.5 — CP2 P1 반영).
     * success=true 이력만 기준으로 체크. 실패 이력이 있으면 당일 재시도 가능.
     */
    boolean existsByUserIdAndPushTypeAndSuccessAndRegDateTimeAfter(
            Long userId, SmartPushType pushType, Boolean success, LocalDateTime since);
}
