package com.sa.baff.repository;

import com.sa.baff.domain.AdPositionConfig;
import com.sa.baff.util.AdWatchLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdPositionConfigRepository extends JpaRepository<AdPositionConfig, Long> {
    Optional<AdPositionConfig> findByPosition(AdWatchLocation position);
}
