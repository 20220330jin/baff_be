package com.sa.baff.domain;

import com.sa.baff.util.AdWatchLocation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ad_watch_events")
@Getter
@Setter
@NoArgsConstructor
public class AdWatchEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdWatchLocation watchLocation;

    /** 참조 ID (체중기록ID, 리뷰ID 등) */
    private Long referenceId;

    /** 토스 광고 응답 코드 (userEarnedReward, dismissed 등) */
    private String tossAdResponse;

    @Column(nullable = false)
    private LocalDateTime regDateTime;

    @PrePersist
    protected void onCreate() {
        this.regDateTime = LocalDateTime.now();
    }

    public AdWatchEvent(Long userId, AdWatchLocation watchLocation, Long referenceId, String tossAdResponse) {
        this.userId = userId;
        this.watchLocation = watchLocation;
        this.referenceId = referenceId;
        this.tossAdResponse = tossAdResponse;
    }
}
