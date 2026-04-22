package com.sa.baff.repository;

import com.sa.baff.domain.FeatureAccessConfig;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface FeatureAccessConfigRepository extends CrudRepository<FeatureAccessConfig, Long> {
    Optional<FeatureAccessConfig> findByFeatureKey(String featureKey);
}
