package com.sa.baff.repository;

import com.sa.baff.domain.AccountLink;
import com.sa.baff.domain.type.AccountLinkStatus;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface AccountLinkRepository extends CrudRepository<AccountLink, Long> {
    Optional<AccountLink> findByProviderAndProviderUserIdAndStatus(
            String provider, String providerUserId, AccountLinkStatus status);

    Optional<AccountLink> findByUserIdAndStatus(Long userId, AccountLinkStatus status);

    boolean existsByUserIdAndStatus(Long userId, AccountLinkStatus status);

    boolean existsByProviderAndProviderUserIdAndStatus(
            String provider, String providerUserId, AccountLinkStatus status);

    /**
     * 평생 1회 제약 판정 (REVOKED 포함 과거 이력 존재 여부).
     * Plan Review Round 2 P1-2 — Optional 대신 existsBy로 복수 row 런타임 예외 방지.
     */
    boolean existsByUserIdAndProvider(Long userId, String provider);

    boolean existsByUserIdAndProviderAndStatus(Long userId, String provider, AccountLinkStatus status);

    /**
     * 동일 외부 identity(provider, providerUserId) 기준 과거 이력 존재 여부 (status 무관).
     * S3-15 P1-2 CP2 Round 2 — 탈퇴 후 재가입으로 "평생 1회" 우회를 차단.
     * 새 Primary userId라도 동일 Toss identity가 과거 ACTIVE/REVOKED 이력 있으면 재연결 불가.
     */
    boolean existsByProviderAndProviderUserId(String provider, String providerUserId);
}
