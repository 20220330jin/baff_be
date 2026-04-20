package com.sa.baff.service.account;

import com.sa.baff.domain.AccountMergeLog;
import com.sa.baff.domain.Piece;
import com.sa.baff.domain.UserB;
import com.sa.baff.domain.type.AccountLinkStatus;
import com.sa.baff.domain.type.UserStatus;
import com.sa.baff.repository.AccountLinkRepository;
import com.sa.baff.repository.AccountMergeLogRepository;
import com.sa.baff.repository.PieceRepository;
import com.sa.baff.repository.UserRepository;
import com.sa.baff.domain.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * 병합 트랜잭션 원자성 검증 (spec §6.3).
 * 중간 단계 실패 시 전체 롤백 확인.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers
class AccountMergeAtomicityTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("baff_test")
            .withUsername("baff")
            .withPassword("baff");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired AccountMergeService mergeService;
    @Autowired UserRepository userRepository;
    @Autowired PieceRepository pieceRepository;
    @Autowired AccountLinkRepository accountLinkRepository;

    @MockitoSpyBean AccountMergeLogRepository mergeLogRepository;
    @MockitoSpyBean AccountLinkRepository spyAccountLinkRepository;

    @AfterEach
    void cleanup() {
        accountLinkRepository.deleteAll();
        pieceRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * 시나리오 1: AccountMergeLog INSERT 실패 → 전체 롤백.
     * AccountLink, Piece 합산, UserB.status 변경 모두 취소.
     */
    @Test
    void rollback_when_merge_log_save_fails() {
        UserB primary = saveUser("primary@test.com", "google_primary", "google");
        UserB secondary = saveUser("secondary@test.com", "12345", "toss");
        savePiece(primary, 100L);
        savePiece(secondary, 50L);

        doThrow(new RuntimeException("injected: merge log")).when(mergeLogRepository).save(any(AccountMergeLog.class));

        assertThrows(RuntimeException.class,
                () -> mergeService.merge(primary.getId(), secondary.getId()));

        // 롤백 검증
        Piece primaryPiece = pieceRepository.findByUser(primary).orElseThrow();
        assertThat(primaryPiece.getBalance()).isEqualTo(100L);
        assertThat(userRepository.findById(secondary.getId()).orElseThrow().getStatus())
                .isEqualTo(UserStatus.ACTIVE);
        assertThat(accountLinkRepository.existsByUserIdAndStatus(primary.getId(), AccountLinkStatus.ACTIVE))
                .isFalse();
    }

    /**
     * 시나리오 2: AccountLink INSERT 실패 → 전체 롤백.
     * Piece 합산 포함 이전 모든 변경 취소.
     */
    @Test
    void rollback_when_account_link_save_fails() {
        UserB primary = saveUser("primary2@test.com", "google_primary2", "google");
        UserB secondary = saveUser("secondary2@test.com", "67890", "toss");
        savePiece(primary, 200L);
        savePiece(secondary, 80L);

        doThrow(new RuntimeException("injected: account link")).when(spyAccountLinkRepository).save(any());

        assertThrows(RuntimeException.class,
                () -> mergeService.merge(primary.getId(), secondary.getId()));

        // 롤백 검증
        Piece primaryPiece = pieceRepository.findByUser(primary).orElseThrow();
        assertThat(primaryPiece.getBalance()).isEqualTo(200L);
        assertThat(userRepository.findById(secondary.getId()).orElseThrow().getStatus())
                .isEqualTo(UserStatus.ACTIVE);
    }

    /**
     * 시나리오 3: 정상 병합 완료 검증 (smoke test).
     * Piece 합산, AccountLink 생성, UserB.status=MERGED.
     */
    @Test
    void merge_succeeds_with_correct_state() {
        UserB primary = saveUser("primary3@test.com", "google_primary3", "google");
        UserB secondary = saveUser("secondary3@test.com", "99999", "toss");
        savePiece(primary, 300L);
        savePiece(secondary, 120L);

        mergeService.merge(primary.getId(), secondary.getId());

        // 검증
        Piece primaryPiece = pieceRepository.findByUser(primary).orElseThrow();
        assertThat(primaryPiece.getBalance()).isEqualTo(420L);
        assertThat(userRepository.findById(secondary.getId()).orElseThrow().getStatus())
                .isEqualTo(UserStatus.MERGED);
        assertThat(accountLinkRepository.existsByUserIdAndStatus(primary.getId(), AccountLinkStatus.ACTIVE))
                .isTrue();
    }

    private UserB saveUser(String email, String socialId, String provider) {
        UserB user = new UserB(email, null, null, socialId, provider, null);
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(Role.USER);
        user.setNickname("테스트유저");
        return userRepository.save(user);
    }

    private Piece savePiece(UserB user, long balance) {
        Piece piece = new Piece(user);
        piece.setBalance(balance);
        return pieceRepository.save(piece);
    }
}
