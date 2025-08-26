package com.sa.baff.repository;

import com.sa.baff.domain.Weight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 체중 관리 기본 Repository
 */
public interface WeightRepository extends JpaRepository<Weight, Long>, WeightRepositoryCustom {

    List<Weight> findByUserId(long userId);

}
