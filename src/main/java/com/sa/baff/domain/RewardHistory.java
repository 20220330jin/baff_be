package com.sa.baff.domain;

import com.sa.baff.util.RewardStatus;
import com.sa.baff.util.RewardType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reward_histories")
@Getter
@Setter
@NoArgsConstructor
public class RewardHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RewardType rewardType;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RewardStatus status;

    /** 참조 ID (체중기록ID, 리뷰ID 등) */
    private Long referenceId;

    /** 에러 메시지 (실패 시) */
    private String errorMessage;

    public RewardHistory(Long userId, RewardType rewardType, Integer amount,
                         RewardStatus status, Long referenceId) {
        this.userId = userId;
        this.rewardType = rewardType;
        this.amount = amount;
        this.status = status;
        this.referenceId = referenceId;
    }
}
