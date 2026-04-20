package com.sa.baff.domain;

import com.sa.baff.domain.type.AccountLinkStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 외부 계정 연결 (spec §3.1, §3.6).
 * Primary UserB ↔ 토스 socialId 매핑. partial unique는 SQL migration으로 관리.
 */
@Entity
@Table(name = "account_links",
        indexes = {
                @Index(name = "ix_account_links_user_status", columnList = "user_id,status")
        })
@Getter
@Setter
@NoArgsConstructor
public class AccountLink extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountLinkStatus status;

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    public AccountLink(Long userId, String provider, String providerUserId) {
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.status = AccountLinkStatus.ACTIVE;
        this.linkedAt = LocalDateTime.now();
    }

    public void revoke() {
        this.status = AccountLinkStatus.REVOKED;
        this.revokedAt = LocalDateTime.now();
    }
}
