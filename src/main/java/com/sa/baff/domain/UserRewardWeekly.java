package com.sa.baff.domain;

import com.sa.baff.util.DateTimeUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * S6-28 주간 마일스톤 달성 플래그 테이블.
 * 체중기록 횟수 3/5/7회 달성 시 각 1회씩 보너스 지급 dedup용.
 *
 * 기존 WeeklyMissionProgress(UI 노출 미션)와는 별도:
 *  - Mission: 사용자가 UI에서 진행도를 확인하고 "보상 받기" 버튼 수동 수령
 *  - Weekly Milestone: 체중 기록 저장 시점에 자동 백그라운드 지급 (S6-28 gap §5-C L1+L2)
 */
@Entity
@Table(name = "user_reward_weekly",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "week_start_date"}))
@Getter
@Setter
@NoArgsConstructor
public class UserRewardWeekly extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 해당 주의 월요일 날짜 */
    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "milestone_3_claimed", nullable = false)
    private Boolean milestone3Claimed = false;

    @Column(name = "milestone_5_claimed", nullable = false)
    private Boolean milestone5Claimed = false;

    @Column(name = "milestone_7_claimed", nullable = false)
    private Boolean milestone7Claimed = false;

    public UserRewardWeekly(Long userId, LocalDate weekStartDate) {
        super(DateTimeUtils.now(), DateTimeUtils.now());
        this.userId = userId;
        this.weekStartDate = weekStartDate;
    }
}
