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
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE);
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
                    .filter(p -> p.getStatus() == UserStatus.ACTIVE);
        }

        // 4. ACTIVE만 허용
        return user.getStatus() == UserStatus.ACTIVE ? Optional.of(user) : Optional.empty();
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
