package com.sa.baff.domain;

import com.sa.baff.domain.type.LinkTokenStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 계정 연결 일회용 토큰 (spec §4.1, §4.3).
 * TTL 5분. DB 저장 (MVP). 향후 Redis 마이그레이션 가능.
 */
@Entity
@Table(name = "link_tokens")
@Getter
@Setter
@NoArgsConstructor
public class LinkToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String token;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LinkTokenStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "idempotency_response", columnDefinition = "TEXT")
    private String idempotencyResponse;

    public LinkToken(String token, Long userId, LocalDateTime expiresAt) {
        this.token = token;
        this.userId = userId;
        this.status = LinkTokenStatus.PENDING;
        this.expiresAt = expiresAt;
    }
}
