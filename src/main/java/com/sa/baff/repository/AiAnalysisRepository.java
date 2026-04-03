package com.sa.baff.repository;

import com.sa.baff.domain.AiAnalysis;
import com.sa.baff.util.AiFeatureType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, Long> {

    Optional<AiAnalysis> findByUserIdAndFeatureType(Long userId, AiFeatureType featureType);
}
