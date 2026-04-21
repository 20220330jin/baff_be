package com.sa.baff.service.account;

import com.sa.baff.domain.LinkToken;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Plan v3 Task 1.5-8 — nonce 발급/검증 경로 단위 테스트.
 *
 * Plan Review Round 2 P0 해소 증명:
 *  - prepareLink가 응답 nonce 발급 + LinkToken에 sha256 해시 저장
 *  - confirmLink가 nonce 검증 (유효/무효/null 3 케이스)
 */
@ExtendWith(MockitoExtension.class)
class AccountLinkServiceNonceTest {

    @Mock private AccountLinkedUserResolver accountLinkedUserResolver;
    @Mock private AccountLinkRepository accountLinkRepository;
    @Mock private LinkTokenRepository linkTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private PieceRepository pieceRepository;
    @Mock private WeightRepository weightRepository;
    @Mock private GoalsRepository goalsRepository;
    @Mock private BattleParticipantRepository battleParticipantRepository;
    @Mock private BattleInviteRepository battleInviteRepository;
    @Mock private AccountMergeService accountMergeService;
    @Mock private LinkTokenConsumer linkTokenConsumer;
    @Mock private TossAuthService tossAuthService;

    @InjectMocks private AccountLinkServiceImpl accountLinkService;

    private static final String TOKEN_VALUE = "link-token-xyz";
    private static final String TOSS_SOCIAL_ID = "99999";
    private static final String AUTH_CODE = "auth-code";
    private static final String REFERRER = "changeup";

    private LinkToken linkToken;
    private UserB primary;
    private UserB secondary;

    @BeforeEach
    void setUp() {
        linkToken = new LinkToken(TOKEN_VALUE, 1L, LocalDateTime.now().plusMinutes(5));

        primary = new UserB("p@toss.im", "primary", "img", "kakao_1", "kakao", "WEB");
        setId(primary, 1L);
        primary.setStatus(UserStatus.ACTIVE);

        secondary = new UserB("s@toss.im", "secondary", "img", TOSS_SOCIAL_ID, "toss", "TOSS");
        setId(secondary, 2L);
        secondary.setStatus(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("prepareLink 성공 시 nonce 발급 + LinkToken.prepareNonceHash == sha256(nonce)")
    void prepareLink_issuesNonceAndStoresHash() {
        stubPrepareHappyPath();

        AccountLinkDto.PrepareRequest request =
                new AccountLinkDto.PrepareRequest(TOKEN_VALUE, AUTH_CODE, REFERRER);
        AccountLinkDto.PrepareResponse response = accountLinkService.prepareLink(request);

        assertThat(response.canLink()).isTrue();
        assertThat(response.nonce()).isNotNull();
        assertThat(response.nonce()).hasSize(32);
        assertThat(linkToken.getTossUserKey()).isEqualTo(TOSS_SOCIAL_ID);
        assertThat(linkToken.getPrepareNonceHash()).isEqualTo(sha256Hex(response.nonce()));
    }

    @Test
    @DisplayName("confirmLink 유효 nonce → 정상 병합")
    void confirmLink_validNonce_succeeds() {
        stubPrepareHappyPath();
        AccountLinkDto.PrepareResponse prepareResponse = accountLinkService.prepareLink(
                new AccountLinkDto.PrepareRequest(TOKEN_VALUE, AUTH_CODE, REFERRER));
        String nonce = prepareResponse.nonce();

        when(linkTokenRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(linkTokenRepository.findByToken(TOKEN_VALUE)).thenReturn(Optional.of(linkToken));

        TransactionSynchronizationManager.initSynchronization();
        try {
            AccountLinkDto.ConfirmResponse response = accountLinkService.confirmLink(
                    new AccountLinkDto.ConfirmRequest(TOKEN_VALUE, "idem-1", nonce));

            assertThat(response.success()).isTrue();
            assertThat(response.primaryUserId()).isEqualTo(1L);
            assertThat(linkToken.getStatus()).isEqualTo(LinkTokenStatus.CONFIRMING);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("confirmLink 임의 nonce → INVALID_NONCE 예외")
    void confirmLink_invalidNonce_throwsInvalidNonce() {
        stubPrepareHappyPath();
        accountLinkService.prepareLink(
                new AccountLinkDto.PrepareRequest(TOKEN_VALUE, AUTH_CODE, REFERRER));

        when(linkTokenRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(linkTokenRepository.findByToken(TOKEN_VALUE)).thenReturn(Optional.of(linkToken));

        assertThatThrownBy(() -> accountLinkService.confirmLink(
                new AccountLinkDto.ConfirmRequest(TOKEN_VALUE, "idem-x", "fake-nonce")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("INVALID_NONCE");
    }

    @Test
    @DisplayName("confirmLink null nonce → INVALID_NONCE 예외")
    void confirmLink_nullNonce_throwsInvalidNonce() {
        stubPrepareHappyPath();
        accountLinkService.prepareLink(
                new AccountLinkDto.PrepareRequest(TOKEN_VALUE, AUTH_CODE, REFERRER));

        when(linkTokenRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(linkTokenRepository.findByToken(TOKEN_VALUE)).thenReturn(Optional.of(linkToken));

        assertThatThrownBy(() -> accountLinkService.confirmLink(
                new AccountLinkDto.ConfirmRequest(TOKEN_VALUE, "idem-y", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("INVALID_NONCE");
    }

    // ====== 공통 stub ======
    private void stubPrepareHappyPath() {
        when(linkTokenRepository.findByToken(TOKEN_VALUE)).thenReturn(Optional.of(linkToken));
        when(userRepository.findById(1L)).thenReturn(Optional.of(primary));
        when(tossAuthService.resolveTossUserKey(AUTH_CODE, REFERRER))
                .thenReturn(new TossAuthService.TossUserKeyResult(TOSS_SOCIAL_ID, 99999L, "e", "n"));
        when(userRepository.findBySocialId(TOSS_SOCIAL_ID)).thenReturn(Optional.of(secondary));

        // detectBlockReason false chain
        when(accountLinkRepository.existsByUserIdAndProvider(anyLong(), anyString())).thenReturn(false);
        when(accountLinkRepository.existsByProviderAndProviderUserIdAndStatus(
                anyString(), anyString(), eq(AccountLinkStatus.ACTIVE))).thenReturn(false);
        lenient().when(battleParticipantRepository.existsActiveByUserId(anyLong(), org.mockito.ArgumentMatchers.anyList())).thenReturn(false);
        lenient().when(battleInviteRepository.existsPendingByUserId(anyLong(), org.mockito.ArgumentMatchers.any())).thenReturn(false);

        // Diff 계산 stubs
        lenient().when(pieceRepository.findByUser(primary)).thenReturn(Optional.empty());
        lenient().when(pieceRepository.findByUser(secondary)).thenReturn(Optional.empty());
        lenient().when(weightRepository.findByUserId(anyLong())).thenReturn(new ArrayList<>());
        lenient().when(battleParticipantRepository.countByUserId(anyLong())).thenReturn(0);
        lenient().when(goalsRepository.findByUserIdAndDelYnAndEndDateGreaterThanEqual(
                anyLong(), eq('N'), org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of(new ArrayList<>()));
    }

    private static void setId(UserB user, Long id) {
        try {
            Field f = UserB.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
