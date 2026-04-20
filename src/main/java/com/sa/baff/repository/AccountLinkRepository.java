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
}
