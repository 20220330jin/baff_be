package com.sa.baff.domain;

import com.sa.baff.util.AdWatchLocation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 자체 배너광고 (공지사항 배너 / 외부 앱 광고).
 *
 * 토스 광고 미수신 시 fallback 노출. 어드민에서 CRUD.
 * 유형:
 *  - NOTICE: 자사 공지사항 배너 (linkUrl로 noticeId path 또는 외부 url)
 *  - EXTERNAL_APP: 외부 앱 광고 (예: 나만그래 앱 이동, intoss://onlyme 등)
 */
@Entity
@Table(name = "self_banner_ads")
@Getter
@Setter
@NoArgsConstructor
public class SelfBannerAd extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** NOTICE | EXTERNAL_APP */
    @Column(nullable = false, length = 30)
    private String bannerType;

    /** 노출 위치 (AdWatchLocation 재사용) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AdWatchLocation position;

    @Column(nullable = false, length = 100)
    private String title;

    /** 배너 이미지 URL */
    @Column(nullable = false, length = 500)
    private String imageUrl;

    /** 클릭 시 이동 URL (intoss://, https://, /notice/123 등) */
    @Column(nullable = false, length = 500)
    private String linkUrl;

    /** 활성화 여부 */
    @Column(nullable = false)
    private Boolean enabled = true;

    /** 정렬/우선순위 (낮을수록 먼저) */
    @Column(nullable = false)
    private Integer priority = 100;

    /** 유저당 1일 노출 상한 (null = 무제한) */
    private Integer dailyImpressionLimit;
}
