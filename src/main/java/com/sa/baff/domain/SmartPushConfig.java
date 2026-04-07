package com.sa.baff.domain;

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

    /** 발송 메시지 제목 */
    private String title;

    /** 발송 메시지 본문 */
    @Column(columnDefinition = "TEXT")
    private String body;

    /** 딥링크 URL (미니앱 내 경로) */
    private String deepLink;

    /** 대상 추출 기준 (일수): 예) 7이면 7일 이상 미환전 */
    private Integer thresholdDays;

    /** cron 표현식 (참고용) */
    private String cronExpression;
}
