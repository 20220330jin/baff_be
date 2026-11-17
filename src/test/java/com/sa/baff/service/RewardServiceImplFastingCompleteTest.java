package com.sa.baff.service;

import com.sa.baff.domain.Piece;
import com.sa.baff.domain.RewardConfig;
import com.sa.baff.domain.RewardHistory;
import com.sa.baff.domain.UserB;
import com.sa.baff.domain.UserRewardDaily;
import com.sa.baff.repository.AdWatchEventRepository;
import com.sa.baff.repository.PieceRepository;
import com.sa.baff.repository.PieceTransactionRepository;
import com.sa.baff.repository.RewardConfigRepository;
import com.sa.baff.repository.RewardHistoryRepository;
import com.sa.baff.repository.UserRewardDailyRepository;
import com.sa.baff.service.account.AccountLinkedUserResolver;
import com.sa.baff.util.RewardStatus;
import com.sa.baff.util.RewardType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M-043 — 단식 완료 보상을 예외 없이 지급한다.
 *
 * 단식 서비스와 보상 서비스가 같은 트랜잭션이라, 보상 쪽이 예외를 던지면
 * 호출자가 잡아도 종료 저장까지 취소된다(spec §12 자체감사 1). 그래서 지급 못 하는
 * 경우는 모두 0g 을 돌려주고 예외를 던지지 않는다.
 */
@ExtendWith(MockitoExtension.class)
class RewardServiceImplFastingCompleteTest {

    @Mock private AccountLinkedUserResolver accountLinkedUserResolver;
    @Mock private PieceRepository pieceRepository;
    @Mock private PieceTransactionRepository pieceTransactionRepository;
    @Mock private RewardConfigRepository rewardConfigRepository;
    @Mock private RewardHistoryRepository rewardHistoryRepository;
    @Mock private UserRewardDailyRepository userRewardDailyRepository;
    @Mock private AdWatchEventRepository adWatchEventRepository;

    @InjectMocks private RewardServiceImpl rewardService;

    private static final Long USER_ID = 42L;
    private static final Long RECORD_ID = 7L;
    private static final String SOCIAL_ID = "toss_42";
    private UserB user;

    @BeforeEach
    void setUp() throws Exception {
        user = new UserB("user@toss.im", "tester", "img", SOCIAL_ID, "toss", "TOSS");
        Field f = UserB.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(user, USER_ID);
        lenient().when(accountLinkedUserResolver.resolveActiveUserBySocialId(SOCIAL_ID))
                .thenReturn(Optional.of(user));
    }

    private RewardConfig fastingConfig() {
        RewardConfig c = new RewardConfig();
        c.setRewardType(RewardType.FASTING_COMPLETE);
        c.setAmount(1);
        c.setProbability(100);
        c.setIsFixed(true);
        c.setEnabled(true);
        c.setDailyLimit(1);
        return c;
    }

    private void givenActiveConfig() {
        when(rewardConfigRepository.findActiveConfigs(RewardType.FASTING_COMPLETE))
                .thenReturn(List.of(fastingConfig()));
    }

    private void givenAlreadyGrantedForRecord(boolean granted) {
        when(rewardHistoryRepository.existsByReferenceIdAndRewardTypeAndStatusAndDelYn(
                RECORD_ID, RewardType.FASTING_COMPLETE, RewardStatus.SUCCESS, 'N')).thenReturn(granted);
    }

    @Test
    @DisplayName("오늘 한도를 이미 채웠으면 0g 을 돌려주고 예외를 던지지 않는다")
    void limitReached_returnsZero_noThrow() {
        givenActiveConfig();
        givenAlreadyGrantedForRecord(false);
        UserRewardDaily daily = new UserRewardDaily(USER_ID, LocalDate.now(), RewardType.FASTING_COMPLETE);
        daily.increment(1);
        when(userRewardDailyRepository.findByUserIdAndRewardDateAndRewardType(
                USER_ID, LocalDate.now(), RewardType.FASTING_COMPLETE)).thenReturn(Optional.of(daily));

        int earned = rewardService.tryGrantFastingCompleteReward(SOCIAL_ID, RECORD_ID);

        assertThat(earned).isZero();
        verify(rewardHistoryRepository, never()).save(any(RewardHistory.class));
        verify(pieceRepository, never()).save(any());
    }

    @Test
    @DisplayName("같은 단식 기록에 이미 지급했으면 0g — 다시 주지 않는다")
    void alreadyGrantedForRecord_returnsZero() {
        givenActiveConfig();
        givenAlreadyGrantedForRecord(true);

        int earned = rewardService.tryGrantFastingCompleteReward(SOCIAL_ID, RECORD_ID);

        assertThat(earned).isZero();
        verify(rewardHistoryRepository, never()).save(any(RewardHistory.class));
        verify(pieceRepository, never()).save(any());
    }

    @Test
    @DisplayName("보상이 꺼져 있으면 0g")
    void disabled_returnsZero() {
        when(rewardConfigRepository.findActiveConfigs(RewardType.FASTING_COMPLETE)).thenReturn(List.of());

        int earned = rewardService.tryGrantFastingCompleteReward(SOCIAL_ID, RECORD_ID);

        assertThat(earned).isZero();
        verify(rewardHistoryRepository, never()).save(any(RewardHistory.class));
    }

    @Test
    @DisplayName("정상 — 설정 금액을 지급하고 단식 기록 번호로 지급 이력을 남긴다")
    void normal_grantsAmount() {
        givenActiveConfig();
        givenAlreadyGrantedForRecord(false);
        when(userRewardDailyRepository.findByUserIdAndRewardDateAndRewardType(
                USER_ID, LocalDate.now(), RewardType.FASTING_COMPLETE)).thenReturn(Optional.empty());
        when(userRewardDailyRepository.save(any(UserRewardDaily.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pieceRepository.findByUser(user)).thenReturn(Optional.of(new Piece(user)));

        int earned = rewardService.tryGrantFastingCompleteReward(SOCIAL_ID, RECORD_ID);

        assertThat(earned).isEqualTo(1);
        ArgumentCaptor<RewardHistory> cap = ArgumentCaptor.forClass(RewardHistory.class);
        verify(rewardHistoryRepository).save(cap.capture());
        assertThat(cap.getValue().getReferenceId()).isEqualTo(RECORD_ID);
        assertThat(cap.getValue().getStatus()).isEqualTo(RewardStatus.SUCCESS);
        assertThat(cap.getValue().getAmount()).isEqualTo(1);
    }
}
