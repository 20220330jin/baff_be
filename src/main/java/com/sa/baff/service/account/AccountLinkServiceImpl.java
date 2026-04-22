package com.sa.baff.service.account;

import com.sa.baff.domain.LinkToken;
import com.sa.baff.domain.Piece;
import com.sa.baff.domain.UserB;
import com.sa.baff.domain.type.AccountLinkStatus;
import com.sa.baff.domain.type.LinkTokenStatus;
import com.sa.baff.domain.type.UserStatus;
import com.sa.baff.model.dto.AccountLinkDto;
import com.sa.baff.repository.AccountLinkRepository;
import com.sa.baff.repository.BattleInviteRepository;
import com.sa.baff.repository.BattleParticipantRepository;
import com.sa.baff.repository.GoalsRepository;
import com.sa.baff.repository.LinkTokenRepository;
import com.sa.baff.repository.PieceRepository;
import com.sa.baff.repository.UserRepository;
import com.sa.baff.repository.WeightRepository;
import com.sa.baff.service.TossAuthService;
import com.sa.baff.util.BattleStatus;
import com.sa.baff.util.InviteStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    private final BattleInviteRepository battleInviteRepository;
    private final AccountMergeService accountMergeService;
    private final LinkTokenConsumer linkTokenConsumer;
    private final TossAuthService tossAuthService;

    private final SecureRandom secureRandom = new SecureRandom();
    private static final int NONCE_BYTE_LENGTH = 24;

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

        // 2. Primary 조회
        UserB primary = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalStateException("PRIMARY_NOT_FOUND"));

        // 3. Toss authorizationCode → tossUserKey (mTLS 내부 교환, FE 미노출)
        TossAuthService.TossUserKeyResult tossResult =
                tossAuthService.resolveTossUserKey(request.authorizationCode(), request.referrer());
        String tossSocialId = tossResult.tossUserKey();

        // 4. Secondary 조회
        UserB secondary = userRepository.findBySocialId(tossSocialId)
                .orElseThrow(() -> new IllegalStateException("SECONDARY_NOT_FOUND"));

        // 5. 차단 조건 검증
        String blockReason = detectBlockReason(primary, secondary);
        if (blockReason != null) {
            return new AccountLinkDto.PrepareResponse(false, null, null, blockReason, null);
        }

        // 6. nonce 발급 + 해시 저장 + tossUserKey 저장 (Plan Review Round 2 P0 해소)
        String noncePlain = generateNonce();
        token.setTossUserKey(tossSocialId);
        token.setPrepareNonceHash(sha256Hex(noncePlain));

        // 7. Diff 계산 + 응답 (nonce 평문 반환)
        AccountLinkDto.Diff diff = calculateDiff(primary, secondary);
        AccountLinkDto.Warnings warnings = new AccountLinkDto.Warnings(
                primary.getNickname(), true);

        return new AccountLinkDto.PrepareResponse(true, diff, warnings, null, noncePlain);
    }

    private String detectBlockReason(UserB primary, UserB secondary) {
        if (primary.getStatus() == UserStatus.WITHDRAWN) {
            return "PRIMARY_WITHDRAWN";
        }
        // 평생 1회 제약 (Plan Review Round 2 P1-2 — existsBy 기반):
        //   ACTIVE 존재 → ALREADY_LINKED
        //   ACTIVE 없지만 과거 REVOKED 이력 존재 → LIFETIME_LIMIT_EXCEEDED
        if (accountLinkRepository.existsByUserIdAndProvider(primary.getId(), PROVIDER_TOSS)) {
            boolean hasActive = accountLinkRepository.existsByUserIdAndProviderAndStatus(
                    primary.getId(), PROVIDER_TOSS, AccountLinkStatus.ACTIVE);
            return hasActive ? "ALREADY_LINKED" : "LIFETIME_LIMIT_EXCEEDED";
        }
        if (accountLinkRepository.existsByProviderAndProviderUserIdAndStatus(
                PROVIDER_TOSS, secondary.getSocialId(), AccountLinkStatus.ACTIVE)) {
            return "TOSS_ALREADY_LINKED";
        }
        if (battleParticipantRepository.existsActiveByUserId(primary.getId(), ACTIVE_BATTLE_STATUSES)
                || battleParticipantRepository.existsActiveByUserId(secondary.getId(), ACTIVE_BATTLE_STATUSES)
                || battleInviteRepository.existsPendingByUserId(primary.getId(), InviteStatus.PENDING, ACTIVE_BATTLE_STATUSES)
                || battleInviteRepository.existsPendingByUserId(secondary.getId(), InviteStatus.PENDING, ACTIVE_BATTLE_STATUSES)) {
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
        // 1. 토큰 비관적 락 획득 (S3-15 P1-1 CP2 Round 2)
        //    동시 confirm 요청을 토큰 단위로 직렬화 → idempotencyResponse 캐시 경로가 안정적으로 히트.
        //    락을 못 잡고 대기하던 요청은 선행 커밋 후 같은 토큰 row를 읽어 아래 멱등 캐시로 분기.
        LinkToken token = linkTokenRepository.findByTokenForUpdate(request.linkToken())
                .orElseThrow(() -> new IllegalStateException("TOKEN_EXPIRED"));

        // 2. 멱등성: 이미 처리된 요청이면 같은 응답 반환 (락 아래 판정이라 race-safe)
        if (token.getIdempotencyResponse() != null) {
            return parseConfirmResponse(token.getIdempotencyResponse());
        }

        // 3. status 판정 (락 아래이므로 CONFIRMING은 이론상 희박. lock_until 만료 등 엣지 케이스 대비)
        //    - CONFIRMING: 이전 요청이 중단되었으나 idempotencyResponse 미커밋 → 재시도 시그널 IN_PROGRESS
        //    - PENDING 아닌 그 외 상태 / 만료: TOKEN_EXPIRED
        if (token.getStatus() == LinkTokenStatus.CONFIRMING) {
            throw new IllegalStateException("IN_PROGRESS");
        }
        if (token.getStatus() != LinkTokenStatus.PENDING
                || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("TOKEN_EXPIRED");
        }

        // 3. prepare에서 저장된 tossUserKey + nonce 해시 로드 (필수)
        String tossSocialId = token.getTossUserKey();
        String storedNonceHash = token.getPrepareNonceHash();
        if (tossSocialId == null || storedNonceHash == null) {
            throw new IllegalStateException("PREPARE_REQUIRED");
        }

        // 4. nonce 검증 (Plan Review Round 2 P0 — Secondary 소유/동의 증명)
        if (request.nonce() == null || !sha256Hex(request.nonce()).equals(storedNonceHash)) {
            throw new IllegalStateException("INVALID_NONCE");
        }

        // 5. Primary/Secondary 조회 + 비관적 락
        // S3-15 P1-2: ID 정렬 순서로 SELECT FOR UPDATE → deadlock 방지 + TOCTOU 창 제거
        Long primaryId = token.getUserId();
        Long secondaryId = userRepository.findBySocialId(tossSocialId)
                .orElseThrow(() -> new IllegalStateException("SECONDARY_NOT_FOUND"))
                .getId();

        Long firstId = Math.min(primaryId, secondaryId);
        Long secondId = Math.max(primaryId, secondaryId);
        UserB first = userRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new IllegalStateException(
                        primaryId.equals(firstId) ? "PRIMARY_NOT_FOUND" : "SECONDARY_NOT_FOUND"));
        UserB second = userRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new IllegalStateException(
                        primaryId.equals(secondId) ? "PRIMARY_NOT_FOUND" : "SECONDARY_NOT_FOUND"));
        UserB primary = primaryId.equals(firstId) ? first : second;
        UserB secondary = primaryId.equals(firstId) ? second : first;

        // 6. TOCTOU 재검증
        String blockReason = detectBlockReason(primary, secondary);
        if (blockReason != null) {
            throw new IllegalStateException(blockReason);
        }

        // 7. 토큰 CONFIRMING 전환 + idempotencyKey 저장 (중복 실행 방지)
        token.setStatus(LinkTokenStatus.CONFIRMING);
        token.setIdempotencyKey(request.idempotencyKey());

        // 8. 병합 트랜잭션
        accountMergeService.merge(primary.getId(), secondary.getId());

        // 9. 응답 구성 + idempotency response 저장
        LocalDateTime linkedAt = LocalDateTime.now();
        String responseJson = String.format(
                "{\"success\":true,\"primaryUserId\":%d,\"linkedAt\":\"%s\"}",
                primary.getId(), linkedAt);
        token.setIdempotencyResponse(responseJson);

        // 10. 커밋 후 CONSUMED 처리 (merge 트랜잭션과 독립)
        Long tokenId = token.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                linkTokenConsumer.consume(tokenId);
            }
        });

        return new AccountLinkDto.ConfirmResponse(true, primary.getId(), linkedAt);
    }

    @Override
    public void dismissLinkBanner(String socialId) {
        UserB user = accountLinkedUserResolver.resolveActiveUserBySocialId(socialId)
                .orElseThrow(() -> new IllegalStateException("USER_NOT_FOUND"));
        user.setAccountLinkBannerDismissedAt(LocalDateTime.now());
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

    private String generateNonce() {
        byte[] bytes = new byte[NONCE_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("sha256 unavailable", e);
        }
    }
}
