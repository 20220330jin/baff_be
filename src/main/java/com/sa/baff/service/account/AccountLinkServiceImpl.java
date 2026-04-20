package com.sa.baff.service.account;

import com.sa.baff.domain.LinkToken;
import com.sa.baff.domain.UserB;
import com.sa.baff.domain.type.AccountLinkStatus;
import com.sa.baff.model.dto.AccountLinkDto;
import com.sa.baff.repository.AccountLinkRepository;
import com.sa.baff.repository.LinkTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * 계정 통합 서비스 구현 (spec §4.1~§4.4).
 *
 * Task 9: issueLinkToken (이 커밋).
 * Task 10: prepareLink — 차단 분기 + Diff 계산.
 * Task 12: confirmLink — 병합 트랜잭션 오케스트레이터.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AccountLinkServiceImpl implements AccountLinkService {

    private static final int TOKEN_TTL_SECONDS = 300;
    private static final int TOKEN_BYTE_LENGTH = 48;

    private final AccountLinkedUserResolver accountLinkedUserResolver;
    private final AccountLinkRepository accountLinkRepository;
    private final LinkTokenRepository linkTokenRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public AccountLinkDto.IssueTokenResponse issueLinkToken(String primarySocialId) {
        UserB user = accountLinkedUserResolver.resolveActiveUserBySocialId(primarySocialId)
                .orElseThrow(() -> new IllegalStateException("USER_NOT_FOUND"));

        // Primary가 이미 연결됐는지 (spec M4: 평생 1회 Primary 기준)
        if (accountLinkRepository.existsByUserIdAndStatus(user.getId(), AccountLinkStatus.ACTIVE)) {
            throw new IllegalStateException("ALREADY_LINKED");
        }

        String token = generateSecureToken();
        LinkToken entity = new LinkToken(token, user.getId(),
                LocalDateTime.now().plusSeconds(TOKEN_TTL_SECONDS));
        linkTokenRepository.save(entity);

        return new AccountLinkDto.IssueTokenResponse(token, TOKEN_TTL_SECONDS);
    }

    @Override
    public AccountLinkDto.PrepareResponse prepareLink(AccountLinkDto.PrepareRequest request) {
        // Task 10에서 구현
        throw new UnsupportedOperationException("Implemented in Task 10");
    }

    @Override
    public AccountLinkDto.ConfirmResponse confirmLink(AccountLinkDto.ConfirmRequest request) {
        // Task 12에서 구현
        throw new UnsupportedOperationException("Implemented in Task 12");
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
