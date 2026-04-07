package com.sa.baff.domain;

import com.sa.baff.util.MissionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "weekly_mission_progress",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "week_start_date", "mission_type"}))
@Getter
@Setter
@NoArgsConstructor
public class WeeklyMissionProgress extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 해당 주의 월요일 날짜 */
    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "mission_type", nullable = false)
    private MissionType missionType;

    /** 현재 진행 횟수 */
    @Column(nullable = false)
    private Integer currentCount = 0;

    /** 목표 횟수 */
    @Column(nullable = false)
    private Integer targetCount;

    /** 미션 완료 여부 */
    @Column(nullable = false)
    private Boolean completed = false;

    /** 보상 수령 여부 */
    @Column(nullable = false)
    private Boolean rewardClaimed = false;

    public WeeklyMissionProgress(Long userId, LocalDate weekStartDate, MissionType missionType, Integer targetCount) {
        this.userId = userId;
        this.weekStartDate = weekStartDate;
        this.missionType = missionType;
        this.targetCount = targetCount;
        this.currentCount = 0;
        this.completed = false;
        this.rewardClaimed = false;
    }

    public void increment() {
        this.currentCount++;
        if (this.currentCount >= this.targetCount) {
            this.completed = true;
        }
    }
}
