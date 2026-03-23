package com.sa.baff.domain;

import com.sa.baff.util.RewardType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "user_reward_dailies",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "reward_date", "reward_type"}))
@Getter
@Setter
@NoArgsConstructor
public class UserRewardDaily extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "reward_date", nullable = false)
    private LocalDate rewardDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false)
    private RewardType rewardType;

    /** 오늘 해당 타입 적립 횟수 */
    @Column(nullable = false)
    private Integer count = 0;

    /** 오늘 해당 타입 총 적립량 */
    @Column(nullable = false)
    private Long totalAmount = 0L;

    public UserRewardDaily(Long userId, LocalDate rewardDate, RewardType rewardType) {
        this.userId = userId;
        this.rewardDate = rewardDate;
        this.rewardType = rewardType;
        this.count = 0;
        this.totalAmount = 0L;
    }

    public void increment(long amount) {
        this.count++;
        this.totalAmount += amount;
    }
}
