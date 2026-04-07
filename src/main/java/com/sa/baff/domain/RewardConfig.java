package com.sa.baff.domain;

import com.sa.baff.util.RewardType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reward_configs")
@Getter
@Setter
@NoArgsConstructor
public class RewardConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RewardType rewardType;

    /** 지급 금액 */
    @Column(nullable = false)
    private Integer amount;

    /** 확률 (0~100). 고정 금액이면 100 */
    @Column(nullable = false)
    private Integer probability = 100;

    /** 일일 제한 횟수 (null이면 제한 없음) */
    private Integer dailyLimit;

    /** 스트릭/마일스톤 기준값 (ex: 7일, 14일) */
    private Integer threshold;

    /** 고정 금액 여부 */
    @Column(nullable = false)
    private Boolean isFixed = true;

    /** 활성화 여부 */
    @Column(nullable = false)
    private Boolean enabled = true;

    /** 설명 */
    private String description;

    /** 쿨타임 (분 단위, null이면 쿨타임 없음) */
    private Integer cooldownMinutes;
}
