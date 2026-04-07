package com.sa.baff.repository;

import com.sa.baff.domain.SmartPushHistory;
import com.sa.baff.util.SmartPushType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmartPushHistoryRepository extends JpaRepository<SmartPushHistory, Long> {

    Page<SmartPushHistory> findByPushTypeOrderByRegDateTimeDesc(SmartPushType pushType, Pageable pageable);

    Page<SmartPushHistory> findAllByOrderByRegDateTimeDesc(Pageable pageable);
}
