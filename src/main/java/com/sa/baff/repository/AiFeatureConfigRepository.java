package com.sa.baff.repository;

import com.sa.baff.domain.AiFeatureConfig;
import com.sa.baff.util.AiFeatureType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiFeatureConfigRepository extends JpaRepository<AiFeatureConfig, Long> {

    Optional<AiFeatureConfig> findByFeatureType(AiFeatureType featureType);

    List<AiFeatureConfig> findAll();
}
