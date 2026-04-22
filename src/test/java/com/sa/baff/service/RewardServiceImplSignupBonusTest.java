package com.sa.baff.service;

import com.sa.baff.domain.Piece;
import com.sa.baff.domain.RewardConfig;
import com.sa.baff.domain.RewardHistory;
import com.sa.baff.domain.UserB;
import com.sa.baff.repository.AdWatchEventRepository;
import com.sa.baff.repository.PieceRepository;
import com.sa.baff.repository.PieceTransactionRepository;
import com.sa.baff.repository.RewardConfigRepository;
import com.sa.baff.repository.RewardHistoryRepository;
import com.sa.baff.repository.UserRewardDailyRepository;
import com.sa.baff.service.account.AccountLinkedUserResolver;
import com.sa.baff.util.PieceTransactionType;
import com.sa.baff.util.RewardStatus;
import com.sa.baff.util.RewardType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * S6-14 SIGNUP_BONUS — `claimSignupBonus` 단위 테스트.
 *
 * TC-1 ~ TC-5: spec.md §7 정의
 * TC-6(Initializer) / TC-7(웹 경로 미적용) 은 별도 위치/문서에서 커버.
 * TC-5-note 동시성은 known risk로 테스트 대상 아님 (spec §7).
 */
@ExtendWith(MockitoExtension.class)
class RewardServiceImplSignupBonusTest {

    @Mock private AccountLinkedUserResolver accountLinkedUserResolver;
    @Mock private PieceRepository pieceRepository;
    @Mock private PieceTransactionRepository pieceTransactionRepository;
    @Mock private RewardConfigRepository rewardConfigRepository;
    @Mock private RewardHistoryRepository rewardHistoryRepository;
    @Mock private UserRewardDailyRepository userRewardDailyRepository;
    @Mock private AdWatchEventRepository adWatchEventRepository;

    @InjectMocks private RewardServiceImpl rewardService;

    private UserB user;
    private static final Long USER_ID = 42L;
    private static final int SIGNUP_AMOUNT = 3;

    @BeforeEach
    void setUp() throws Exception {
        user = new UserB("user@toss.im", "tester", "img", "toss_42", "toss", "TOSS");
        setField(user, "id", USER_ID);
    }

    private RewardConfig signupConfig(int amount) {
        RewardConfig c = new RewardConfig();
        c.setRewardType(RewardType.SIGNUP_BONUS);
        c.setAmount(amount);
        c.setProbability(100);
        c.setIsFixed(true);
        c.setEnabled(true);
        c.setDailyLimit(1);
        return c;
    }

    @Test
    @DisplayName("TC-1: 신규 유저 첫 호출 — SIGNUP_BONUS SUCCESS 1건 저장 + PieceTransaction(REWARD_SIGNUP_BONUS) + Piece balance +3")
    void tc1_firstCall_grants() {
        when(rewardConfigRepository.findActiveConfigs(RewardType.SIGNUP_BONUS))
                .thenReturn(List.of(signupConfig(SIGNUP_AMOUNT)));
        when(rewardHistoryRepository.existsByUserIdAndRewardTypeAndStatusAndDelYn(
                USER_ID, RewardType.SIGNUP_BONUS, RewardStatus.SUCCESS, 'N')).thenReturn(false);
        when(pieceRepository.findByUser(user)).thenReturn(Optional.of(new Piece(user)));

        rewardService.claimSignupBonus(USER_ID, user);

        ArgumentCaptor<RewardHistory> historyCap = ArgumentCaptor.forClass(RewardHistory.class);
        verify(rewardHistoryRepository).save(historyCap.capture());
        RewardHistory saved = historyCap.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getRewardType()).isEqualTo(RewardType.SIGNUP_BONUS);
        assertThat(saved.getAmount()).isEqualTo(SIGNUP_AMOUNT);
        assertThat(saved.getStatus()).isEqualTo(RewardStatus.SUCCESS);

        ArgumentCaptor<com.sa.baff.domain.PieceTransaction> txCap =
                ArgumentCaptor.forClass(com.sa.baff.domain.PieceTransaction.class);
        verify(pieceTransactionRepository).save(txCap.capture());
        assertThat(txCap.getValue().getType()).isEqualTo(PieceTransactionType.REWARD_SIGNUP_BONUS);
        assertThat(txCap.getValue().getAmount()).isEqualTo((long) SIGNUP_AMOUNT);
    }

    @Test
    @DisplayName("TC-2: 기존 유저 (SIGNUP_BONUS SUCCESS 이력 있음) — skip, RewardHistory 저장 안 됨")
    void tc2_alreadyClaimed_skip() {
        when(rewardConfigRepository.findActiveConfigs(RewardType.SIGNUP_BONUS))
                .thenReturn(List.of(signupConfig(SIGNUP_AMOUNT)));
        when(rewardHistoryRepository.existsByUserIdAndRewardTypeAndStatusAndDelYn(
                USER_ID, RewardType.SIGNUP_BONUS, RewardStatus.SUCCESS, 'N')).thenReturn(true);

        rewardService.claimSignupBonus(USER_ID, user);

        verify(rewardHistoryRepository, never()).save(any(RewardHistory.class));
        verify(pieceTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-3: RewardConfig.enabled=false — findActiveConfigs empty, skip")
    void tc3_disabled_skip() {
        when(rewardConfigRepository.findActiveConfigs(RewardType.SIGNUP_BONUS))
                .thenReturn(List.of());

        rewardService.claimSignupBonus(USER_ID, user);

        verify(rewardHistoryRepository, never()).existsByUserIdAndRewardTypeAndStatusAndDelYn(
                any(), any(), any(), any());
        verify(rewardHistoryRepository, never()).save(any(RewardHistory.class));
        verify(pieceTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-4: RewardConfig row 자체 없음 — findActiveConfigs empty, skip (TC-3과 동일 동작)")
    void tc4_missingConfig_skip() {
        when(rewardConfigRepository.findActiveConfigs(RewardType.SIGNUP_BONUS))
                .thenReturn(List.of());

        rewardService.claimSignupBonus(USER_ID, user);

        verify(rewardHistoryRepository, never()).save(any(RewardHistory.class));
    }

    @Test
    @DisplayName("TC-5: 단일 스레드 dedup — 순차 2회 호출 시 총 1건만 저장")
    void tc5_sequentialDoubleCall_dedup() {
        when(rewardConfigRepository.findActiveConfigs(RewardType.SIGNUP_BONUS))
                .thenReturn(List.of(signupConfig(SIGNUP_AMOUNT)));
        // 1회차: exists=false → 저장 / 2회차: exists=true → skip
        when(rewardHistoryRepository.existsByUserIdAndRewardTypeAndStatusAndDelYn(
                USER_ID, RewardType.SIGNUP_BONUS, RewardStatus.SUCCESS, 'N'))
                .thenReturn(false, true);
        when(pieceRepository.findByUser(user)).thenReturn(Optional.of(new Piece(user)));

        rewardService.claimSignupBonus(USER_ID, user);
        rewardService.claimSignupBonus(USER_ID, user);

        verify(rewardHistoryRepository, org.mockito.Mockito.times(1)).save(any(RewardHistory.class));
        verify(pieceTransactionRepository, org.mockito.Mockito.times(1)).save(any());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
