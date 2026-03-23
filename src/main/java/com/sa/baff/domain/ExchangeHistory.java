package com.sa.baff.domain;

import com.sa.baff.util.ExchangeStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "exchange_histories")
@Getter
@Setter
@NoArgsConstructor
public class ExchangeHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** 환전한 그램 수량 */
    @Column(nullable = false)
    private Integer pointAmount;

    /** 토스포인트 환전 금액 (원) */
    @Column(nullable = false)
    private Integer tossAmount;

    /** 광고 시청 여부 */
    @Column(nullable = false)
    private Boolean adWatched = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExchangeStatus status;

    /** 토스 프로모션 Key */
    private String promotionKey;

    /** 토스 트랜잭션 ID */
    private String transactionId;

    /** 실패 시 에러 메시지 */
    private String errorMessage;

    public ExchangeHistory(Long userId, Integer pointAmount, Integer tossAmount, Boolean adWatched) {
        this.userId = userId;
        this.pointAmount = pointAmount;
        this.tossAmount = tossAmount;
        this.adWatched = adWatched;
        this.status = ExchangeStatus.PENDING;
    }
}
