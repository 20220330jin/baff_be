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

    /** 토스 프로모션 코드 (FIRST_ATTENDANCE_BONUS 등 토스포인트 직접 지급 시 기록). */
    @Column(length = 100)
    private String promotionKey;

    /** 토스 프로모션 트랜잭션 ID (지급 추적/조회용). */
    @Column(length = 100)
    private String promotionTransactionId;

    public RewardHistory(Long userId, RewardType rewardType, Integer amount,
                         RewardStatus status, Long referenceId) {
        this.userId = userId;
        this.rewardType = rewardType;
        this.amount = amount;
        this.status = status;
        this.referenceId = referenceId;
    }
}
