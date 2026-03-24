package com.sa.baff.domain;

import com.sa.baff.util.AdWatchLocation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ad_position_config")
@Getter
@Setter
@NoArgsConstructor
public class AdPositionConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    private AdWatchLocation position;

    /** 토스광고 비율 (0~100%) */
    @Column(nullable = false)
    private Integer tossAdRatio = 30;

    /** 토스광고 그룹 ID */
    @Column(length = 100)
    private String tossAdGroupId;

    /** 토스광고 활성 여부 */
    @Column(nullable = false)
    private Boolean isTossAdEnabled = false;

    /** 이미지 배너 광고 그룹 ID */
    @Column(length = 100)
    private String tossImageAdGroupId;

    /** 이미지 배너 광고 비율 (0~100%) */
    @Column(nullable = false)
    private Integer tossImageAdRatio = 0;

    /** 이미지 배너 광고 활성 여부 */
    @Column(nullable = false)
    private Boolean isTossImageAdEnabled = false;

    /** 작은 배너 광고 그룹 ID */
    @Column(length = 100)
    private String tossBannerAdGroupId;

    /** 작은 배너 광고 비율 (0~100%) */
    @Column(nullable = false)
    private Integer tossBannerAdRatio = 0;

    /** 작은 배너 광고 활성 여부 */
    @Column(nullable = false)
    private Boolean isTossBannerAdEnabled = false;
}
