package com.sa.baff.repository;

import com.sa.baff.domain.Goals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 목표 설정 관련 레포지토리 모음
 */
@Repository
public interface GoalsRepository extends JpaRepository<Goals, Long>, GoalsRepositoryCustom {
    /**
     * 유저 ID를 통한 목표 리스트 조
     * @param userId
     * @return
     */
    Optional<List<Goals>> findByUserIdAndDelYn(Long userId, Character delYn);
}
