package com.sa.baff.repository;

import com.sa.baff.domain.UserB;
import com.sa.baff.domain.Weight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 체중 관리 기본 Repository
 */
public interface WeightRepository extends JpaRepository<Weight, Long>, WeightRepositoryCustom {

    List<Weight> findByUserId(long userId);

    // 사용자 및 특정 날짜 범위(시작 시간 ~ 종료 시간)로 체중 기록을 조회
    Optional<Weight> findByUserAndRecordDateBetween(UserB user, LocalDateTime startOfDay, LocalDateTime endOfDay);

    Optional<Weight> findTopByUserOrderByRecordDateDesc(UserB user);

    Optional<Weight> findTopByUserAndRecordDateLessThanEqualOrderByRecordDateDesc(UserB user, LocalDateTime attr0);
}
