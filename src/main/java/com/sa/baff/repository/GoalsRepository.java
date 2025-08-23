package com.sa.baff.repository;

import com.sa.baff.domain.Goals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 목표 설정 관련 레포지토리 모음
 */
@Repository
public interface GoalsRepository extends JpaRepository<Goals, Long>, GoalsRepositoryCustom {
}
