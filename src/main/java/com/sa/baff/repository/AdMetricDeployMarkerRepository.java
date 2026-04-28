package com.sa.baff.repository;

import com.sa.baff.domain.AdMetricDeployMarker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdMetricDeployMarkerRepository extends JpaRepository<AdMetricDeployMarker, Long> {
    List<AdMetricDeployMarker> findAllByOrderByMetricDateAsc();
}
