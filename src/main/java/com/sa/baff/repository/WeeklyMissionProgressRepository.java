package com.sa.baff.repository;

import com.sa.baff.domain.WeeklyMissionProgress;
import com.sa.baff.util.MissionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklyMissionProgressRepository extends JpaRepository<WeeklyMissionProgress, Long> {

    List<WeeklyMissionProgress> findByUserIdAndWeekStartDate(Long userId, LocalDate weekStartDate);

    Optional<WeeklyMissionProgress> findByUserIdAndWeekStartDateAndMissionType(
            Long userId, LocalDate weekStartDate, MissionType missionType);
}
