package com.sa.baff.repository;

import com.sa.baff.domain.SmartPushConfig;
import com.sa.baff.util.SmartPushType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SmartPushConfigRepository extends JpaRepository<SmartPushConfig, Long> {

    Optional<SmartPushConfig> findByPushType(SmartPushType pushType);
}
