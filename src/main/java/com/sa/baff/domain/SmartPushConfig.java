package com.sa.baff.domain;

import com.sa.baff.util.SmartPushTargetStrategy;
import com.sa.baff.util.SmartPushType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "smart_push_configs")
@Getter
@Setter
@NoArgsConstructor
public class SmartPushConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private SmartPushType pushType;

    /** 활성화 여부 */
    @Column(nullable = false)
    private Boolean enabled = false;

    /** 발송 메시지 제목 (토스 콘솔 템플릿에 이미 고정. 기록/참고용) */
    private String title;

    /** 발송 메시지 본문 (토스 콘솔 템플릿에 이미 고정. 기록/참고용) */
    @Column(columnDefinition = "TEXT")
    private String body;

    /** 딥링크 URL (미니앱 내 경로) */
    private String deepLink;

    /** 대상 추출 기준 (일수): 예) 7이면 7일 이상 미환전. targetStrategy=BALANCE_OVER_100G 전용 */
    private Integer thresholdDays;

    /** cron 표현식 (참고용) */
    private String cronExpression;

    /** 토스 콘솔 검수 완료된 템플릿 코드 (spec §3.3 — CP1 반영). blank면 발송 skip */
    @Column(name = "template_code", length = 100)
    private String templateCode;

    /** 대상 선별 전략 (spec §3.2 — CP1 반영). 기본 ALL_TOSS_USERS */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_strategy", nullable = false, length = 50)
    private SmartPushTargetStrategy targetStrategy = SmartPushTargetStrategy.ALL_TOSS_USERS;
}
