package com.sa.baff.domain;

import com.sa.baff.domain.type.BrowserType;
import com.sa.baff.domain.type.DeviceType;
import com.sa.baff.domain.type.OperatingSystemType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class LoginHistory extends BaseEntity {
    @Id
    @GeneratedValue
    @Column(name = "loginHistoryId")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserB user;

    @Column(columnDefinition = "TEXT")
    private String rawUserAgent; // 원본 User-Agent 문자열

    @Enumerated(EnumType.STRING)
    private DeviceType deviceType; // 기기 종류 (PC, MOBILE, TABLET)

    @Enumerated(EnumType.STRING)
    private OperatingSystemType os; // 운영체제 (WINDOWS, MACOS, IOS, ANDROID 등)

    @Enumerated(EnumType.STRING)
    private BrowserType browser; // 브라우저 (CHROME, SAFARI, KAKAOTALK 등)

    @Builder
    public LoginHistory(UserB user, String rawUserAgent, DeviceType deviceType, OperatingSystemType os, BrowserType browser) {
        this.user = user;
        this.rawUserAgent = rawUserAgent;
//        this.deviceType = deviceType;
//        this.os = os;
//        this.browser = browser;
    }
}
