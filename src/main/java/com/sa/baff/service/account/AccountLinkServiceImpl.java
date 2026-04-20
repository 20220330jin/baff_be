package com.sa.baff.service.account;

import com.sa.baff.domain.LinkToken;
import com.sa.baff.domain.Piece;
import com.sa.baff.domain.UserB;
import com.sa.baff.domain.type.AccountLinkStatus;
import com.sa.baff.domain.type.LinkTokenStatus;
import com.sa.baff.domain.type.UserStatus;
import com.sa.baff.model.dto.AccountLinkDto;
import com.sa.baff.repository.AccountLinkRepository;
import com.sa.baff.repository.BattleParticipantRepository;
import com.sa.baff.repository.GoalsRepository;
import com.sa.baff.repository.LinkTokenRepository;
import com.sa.baff.repository.PieceRepository;
import com.sa.baff.repository.UserRepository;
import com.sa.baff.repository.WeightRepository;
import com.sa.baff.util.BattleStatus;
import com.sa.baff.util.TossSocialIdMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

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
    private static final String PROVIDER_TOSS = "toss";
    private static final List<BattleStatus> ACTIVE_BATTLE_STATUSES =
            List.of(BattleStatus.WAITING, BattleStatus.IN_PROGRESS);

    private final AccountLinkedUserResolver accountLinkedUserResolver;
    private final AccountLinkRepository accountLinkRepository;
    private final LinkTokenRepository linkTokenRepository;
    private final UserRepository userRepository;
    private final PieceRepository pieceRepository;
    private final WeightRepository weightRepository;
    private final GoalsRepository goalsRepository;
    private final BattleParticipantRepository battleParticipantRepository;
    private final AccountMergeService accountMergeService;
    private final LinkTokenConsumer linkTokenConsumer;

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
        // 1. 토큰 유효성
        LinkToken token = linkTokenRepository.findByToken(request.linkToken())
                .orElseThrow(() -> new IllegalStateException("TOKEN_EXPIRED"));
        if (token.getStatus() != LinkTokenStatus.PENDING
                || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("TOKEN_EXPIRED");
        }

        // 2. Primary/Secondary 조회
        UserB primary = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalStateException("PRIMARY_NOT_FOUND"));
        String tossSocialId = TossSocialIdMapper.toStoredSocialId(request.tossUserKey());
        UserB secondary = userRepository.findBySocialId(tossSocialId)
                .orElseThrow(() -> new IllegalStateException("SECONDARY_NOT_FOUND"));

        // 3. 차단 조건 검증
        String blockReason = detectBlockReason(primary, secondary);
        if (blockReason != null) {
            return new AccountLinkDto.PrepareResponse(false, null, null, blockReason);
        }

        // 4. Diff 계산
        AccountLinkDto.Diff diff = calculateDiff(primary, secondary);
        AccountLinkDto.Warnings warnings = new AccountLinkDto.Warnings(
                primary.getNickname(), true);

        return new AccountLinkDto.PrepareResponse(true, diff, warnings, null);
    }

    private String detectBlockReason(UserB primary, UserB secondary) {
        if (primary.getStatus() == UserStatus.WITHDRAWN) {
            return "PRIMARY_WITHDRAWN";
        }
        if (accountLinkRepository.existsByUserIdAndStatus(primary.getId(), AccountLinkStatus.ACTIVE)) {
            return "ALREADY_LINKED";
        }
        if (accountLinkRepository.existsByProviderAndProviderUserIdAndStatus(
                PROVIDER_TOSS, secondary.getSocialId(), AccountLinkStatus.ACTIVE)) {
            return "TOSS_ALREADY_LINKED";
        }
        if (battleParticipantRepository.existsActiveByUserId(primary.getId(), ACTIVE_BATTLE_STATUSES)
                || battleParticipantRepository.existsActiveByUserId(secondary.getId(), ACTIVE_BATTLE_STATUSES)) {
            return "ACTIVE_BATTLE";
        }
        return null;
    }

    private AccountLinkDto.Diff calculateDiff(UserB primary, UserB secondary) {
        Optional<Piece> primaryPiece = pieceRepository.findByUser(primary);
        Optional<Piece> secondaryPiece = pieceRepository.findByUser(secondary);
        long primaryBalance = primaryPiece.map(Piece::getBalance).orElse(0L);
        long secondaryBalance = secondaryPiece.map(Piece::getBalance).orElse(0L);

        AccountLinkDto.GramsDiff grams =
                new AccountLinkDto.GramsDiff(secondaryBalance, primaryBalance + secondaryBalance);

        int weightLogs = weightRepository.findByUserId(secondary.getId()).size();
        int battles = battleParticipantRepository.countByUserId(secondary.getId());
        int activeGoals = goalsRepository
                .findByUserIdAndDelYnAndEndDateGreaterThanEqual(
                        secondary.getId(), 'N', LocalDateTime.now())
                .map(List::size)
                .orElse(0);

        return new AccountLinkDto.Diff(grams, weightLogs, battles, activeGoals);
    }

    @Override
    public AccountLinkDto.ConfirmResponse confirmLink(AccountLinkDto.ConfirmRequest request) {
        // 1. 멱등성: 이미 처리된 요청인지 확인
        Optional<LinkToken> existing = linkTokenRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent() && existing.get().getIdempotencyResponse() != null) {
            return parseConfirmResponse(existing.get().getIdempotencyResponse());
        }

        // 2. 토큰 유효성
        LinkToken token = linkTokenRepository.findByToken(request.linkToken())
                .orElseThrow(() -> new IllegalStateException("TOKEN_EXPIRED"));
        if (token.getStatus() != LinkTokenStatus.PENDING
                || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("TOKEN_EXPIRED");
        }

        // 3. Primary/Secondary 조회
        UserB primary = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalStateException("PRIMARY_NOT_FOUND"));
        String tossSocialId = TossSocialIdMapper.toStoredSocialId(request.tossUserKey());
        UserB secondary = userRepository.findBySocialId(tossSocialId)
                .orElseThrow(() -> new IllegalStateException("SECONDARY_NOT_FOUND"));

        // 4. TOCTOU 재검증
        String blockReason = detectBlockReason(primary, secondary);
        if (blockReason != null) {
            throw new IllegalStateException(blockReason);
        }

        // 5. 토큰 CONFIRMING 전환 + idempotencyKey 저장 (중복 실행 방지)
        token.setStatus(LinkTokenStatus.CONFIRMING);
        token.setIdempotencyKey(request.idempotencyKey());

        // 6. 병합 트랜잭션
        accountMergeService.merge(primary.getId(), secondary.getId());

        // 7. 응답 구성 + idempotency response 저장
        LocalDateTime linkedAt = LocalDateTime.now();
        String responseJson = String.format(
                "{\"success\":true,\"primaryUserId\":%d,\"linkedAt\":\"%s\"}",
                primary.getId(), linkedAt);
        token.setIdempotencyResponse(responseJson);

        // 8. 커밋 후 CONSUMED 처리 (merge 트랜잭션과 독립)
        Long tokenId = token.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                linkTokenConsumer.consume(tokenId);
            }
        });

        return new AccountLinkDto.ConfirmResponse(true, primary.getId(), linkedAt);
    }

    private AccountLinkDto.ConfirmResponse parseConfirmResponse(String json) {
        try {
            // {"success":true,"primaryUserId":123,"linkedAt":"2026-04-20T23:00:00"}
            Long primaryUserId = Long.parseLong(json.replaceAll(".*\"primaryUserId\":(\\d+).*", "$1"));
            String linkedAtStr = json.replaceAll(".*\"linkedAt\":\"([^\"]+)\".*", "$1");
            LocalDateTime linkedAt = LocalDateTime.parse(linkedAtStr);
            return new AccountLinkDto.ConfirmResponse(true, primaryUserId, linkedAt);
        } catch (Exception e) {
            throw new IllegalStateException("IDEMPOTENCY_PARSE_ERROR");
        }
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
