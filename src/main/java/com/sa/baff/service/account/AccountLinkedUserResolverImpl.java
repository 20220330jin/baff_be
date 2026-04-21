package com.sa.baff.service.account;

import com.sa.baff.domain.AccountLink;
import com.sa.baff.domain.UserB;
import com.sa.baff.domain.type.AccountLinkStatus;
import com.sa.baff.domain.type.UserStatus;
import com.sa.baff.repository.AccountLinkRepository;
import com.sa.baff.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountLinkedUserResolverImpl implements AccountLinkedUserResolver {

    private static final String PROVIDER_TOSS = "toss";

    private final UserRepository userRepository;
    private final AccountLinkRepository accountLinkRepository;

    @Override
    public Optional<UserB> resolveActiveUserBySocialId(String socialId) {
        // 1. AccountLink.ACTIVE 우선 조회 → Primary UserB 반환
        Optional<AccountLink> link = accountLinkRepository
                .findByProviderAndProviderUserIdAndStatus(PROVIDER_TOSS, socialId, AccountLinkStatus.ACTIVE);
        if (link.isPresent()) {
            return userRepository.findById(link.get().getUserId())
                    .filter(AccountLinkedUserResolverImpl::isActive);
        }

        // 2. 기존 UserB 조회
        Optional<UserB> userOpt = userRepository.findBySocialId(socialId);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        UserB user = userOpt.get();

        // 3. MERGED → primary_user_id로 재조회
        if (user.getStatus() == UserStatus.MERGED && user.getPrimaryUserId() != null) {
            return userRepository.findById(user.getPrimaryUserId())
                    .filter(AccountLinkedUserResolverImpl::isActive);
        }

        // 4. ACTIVE만 허용 (null은 ACTIVE로 간주 — 신규 컬럼 도입 전 기존 row 방어)
        return isActive(user) ? Optional.of(user) : Optional.empty();
    }

    /**
     * null 허용 ACTIVE 판정.
     * 2026-04-21 사건: Hibernate ddl-auto가 status 컬럼을 추가했지만 기존 row는 NULL.
     * Java 엔티티 default는 ACTIVE이므로 null은 ACTIVE로 해석해야 레거시 유저 로그인 가능.
     */
    private static boolean isActive(UserB user) {
        return user.getStatus() == null || user.getStatus() == UserStatus.ACTIVE;
    }

    @Override
    public boolean existsBySocialId(String socialId) {
        if (accountLinkRepository
                .findByProviderAndProviderUserIdAndStatus(PROVIDER_TOSS, socialId, AccountLinkStatus.ACTIVE)
                .isPresent()) {
            return true;
        }
        return userRepository.findBySocialId(socialId).isPresent();
    }

    @Override
    public Optional<UserB> findRawBySocialId(String socialId) {
        return userRepository.findBySocialId(socialId);
    }
}
