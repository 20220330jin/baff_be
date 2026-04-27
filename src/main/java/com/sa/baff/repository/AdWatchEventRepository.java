package com.sa.baff.repository;

import com.sa.baff.domain.AdWatchEvent;
import com.sa.baff.util.AdWatchLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AdWatchEventRepository extends JpaRepository<AdWatchEvent, Long> {

    /** 빈도 제한 enforce용 — 유저 + 위치 + 오늘 범위 노출 수 카운트 (나만그래 패턴) */
    long countByUserIdAndWatchLocationAndRegDateTimeBetween(
            Long userId, AdWatchLocation watchLocation, LocalDateTime from, LocalDateTime to);
}
