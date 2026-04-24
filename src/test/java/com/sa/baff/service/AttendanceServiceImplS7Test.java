package com.sa.baff.service;

import com.sa.baff.domain.AdWatchEvent;
import com.sa.baff.domain.UserAttendance;
import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.AttendanceDto;
import com.sa.baff.model.dto.RewardDto;
import com.sa.baff.repository.AccountLinkRepository;
import com.sa.baff.repository.AdWatchEventRepository;
import com.sa.baff.repository.PieceRepository;
import com.sa.baff.repository.PieceTransactionRepository;
import com.sa.baff.repository.RewardConfigRepository;
import com.sa.baff.repository.RewardHistoryRepository;
import com.sa.baff.repository.UserAttendanceRepository;
import com.sa.baff.service.account.AccountLinkedUserResolver;
import com.sa.baff.util.AdWatchLocation;
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
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * S7-14 — AttendanceServiceImpl 신규/수정 로직 단위 테스트.
 *
 * 범위:
 *  - canSaveStreak 판정 (getStatus 응답 필드)
 *  - saveStreak 조건별 분기 (happy / today 출석 / 어제 출석 / 그저께 무출석)
 *  - 1일 streak 복구 semantics (그저께=1 → 어제(가상)=2)
 *  - grantAdBonus가 rewardService.grantAttendanceAdBonus 위임 + AdWatchEvent 저장
 *
 * 동시성 유니크 충돌 409는 unit test로 재현 불가 → BE-8b(integration)로 분리.
 */
@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplS7Test {

    @Mock private AccountLinkedUserResolver accountLinkedUserResolver;
    @Mock private AccountLinkRepository accountLinkRepository;
    @Mock private PieceRepository pieceRepository;
    @Mock private PieceTransactionRepository pieceTransactionRepository;
    @Mock private UserAttendanceRepository userAttendanceRepository;
    @Mock private RewardConfigRepository rewardConfigRepository;
    @Mock private RewardHistoryRepository rewardHistoryRepository;
    @Mock private AdWatchEventRepository adWatchEventRepository;
    @Mock private MissionService missionService;
    @Mock private RewardService rewardService;

    @InjectMocks private AttendanceServiceImpl attendanceService;

    private UserB user;
    private static final Long USER_ID = 42L;
    private static final String SOCIAL_ID = "social-42";

    @BeforeEach
    void setup() throws Exception {
        user = new UserB("user@toss.im", "tester", "img", SOCIAL_ID, "toss", "TOSS");
        Field idField = UserB.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, USER_ID);
    }

    @Nested
    @DisplayName("getStatus — canSaveStreak 판정")
    class CanSaveStreakTest {

        @Test
        @DisplayName("TC-1: 오늘 미출석 + 어제 미출석 + 그저께 출석 → canSaveStreak=true")
        void canSaveStreak_true_when_conditions_match() {
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            LocalDate dayBefore = today.minusDays(2);

            when(accountLinkedUserResolver.resolveActiveUserBySocialId(SOCIAL_ID)).thenReturn(Optional.of(user));
            when(accountLinkRepository.existsByUserIdAndStatus(any(), any())).thenReturn(false);
            when(userAttendanceRepository.findByUserIdAndAttendanceDate(USER_ID, today)).thenReturn(Optional.empty());
            when(userAttendanceRepository.findByUserIdAndAttendanceDate(USER_ID, yesterday)).thenReturn(Optional.empty());
            when(userAttendanceRepository.findByUserIdAndAttendanceDate(USER_ID, dayBefore))
                    .thenReturn(Optional.of(stubAttendance(dayBefore, 1)));
            when(userAttendanceRepository.findAttendanceDatesInMonth(anyLong(), any(), any())).thenReturn(List.of(dayBefore));
            when(rewardConfigRepository.findActiveConfigs(any())).thenReturn(Collections.emptyList());

            AttendanceDto.statusResponse response = attendanceService.getStatus(SOCIAL_ID);

            assertThat(response.getCanSaveStreak()).isTrue();
            assertThat(response.getAttendedToday()).isFalse();
        }

        @Test
        @DisplayName("TC-2: 오늘 출석했으면 canSaveStreak=false")
        void canSaveStreak_false_when_today_attended() {
            LocalDate today = LocalDate.now();

            when(accountLinkedUserResolver.resolveActiveUserBySocialId(SOCIAL_ID)).thenReturn(Optional.of(user));
            when(accountLinkRepository.existsByUserIdAndStatus(any(), any())).thenReturn(false);
            when(userAttendanceRepository.findByUserIdAndAttendanceDate(USER_ID, today))
                    .thenReturn(Optional.of(stubAttendance(today, 5)));
            when(userAttendanceRepository.findAttendanceDatesInMonth(anyLong(), any(), any())).thenReturn(List.of(today));
            when(rewardConfigRepository.findActiveConfigs(any())).thenReturn(Collections.emptyList());

            AttendanceDto.statusResponse response = attendanceService.getStatus(SOCIAL_ID);

            assertThat(response.getCanSaveStreak()).isFalse();
            assertThat(response.getAttendedToday()).isTrue();
        }

        @Test
        @DisplayName("TC-3: 어제 출석했으면 canSaveStreak=false")
        void canSaveStreak_false_when_yesterday_attended() {
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);

            when(accountLinkedUserResolver.resolveActiveUserBySocialId(SOCIAL_ID)).thenReturn(Optional.of(user));
            when(accountLinkRepository.existsByUserIdAndStatus(any(), any())).thenReturn(false);
            when(userAttendanceRepository.findByUserIdAndAttendanceDate(USER_ID, today)).thenReturn(Optional.empty());
            when(userAttendanceRepository.findByUserIdAndAttendanceDate(USER_ID, yesterday))
                    .thenReturn(Optional.of(stubAttendance(yesterday, 3)));
            when(userAttendanceRepository.findAttendanceDatesInMonth(anyLong(), any(), any())).thenReturn(List.of(yesterday));
            when(rewardConfigRepository.findActiveConfigs(any())).thenReturn(Collections.emptyList());

            AttendanceDto.statusResponse response = attendanceService.getStatus(SOCIAL_ID);

            assertThat(response.getCanSaveStreak()).isFalse();
        }

        @Test
        @DisplayName("TC-4: 그저께 출석 없으면 canSaveStreak=false")
        void canSaveStreak_false_when_no_day_before() {
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            LocalDate dayBefore = today.minusDays(2);

            when(accountLinkedUserResolver.resolveActiveUserBySocialId(SOCIAL_ID)).thenReturn(Optional.of(user));
            when(accountLinkRepository.existsByUserIdAndStatus(any(), any())).thenReturn(false);
            when(userAttendanceRepository.findByUserIdAndAttendanceDate(USER_ID, today)).thenReturn(Optional.empty());
            when(userAttendanceRepository.findByUserIdAndAttendanceDate(USER_ID, yesterday)).thenReturn(Optional.empty());
            when(userAttendanceRepository.findByUserIdAndAttendanceDate(USER_ID, dayBefore)).thenReturn(Optional.empty());
            when(userAttendanceRepository.findAttendanceDatesInMonth(anyLong(), any(), any())).thenReturn(Collections.emptyList());
            when(rewardConfigRepository.findActiveConfigs(any())).thenReturn(Collections.emptyList());

            AttendanceDto.statusResponse response = attendanceService.getStatus(SOCIAL_ID);

            assertThat(response.getCanSaveStreak()).isFalse();
        }
    }

    @Nested
    @DisplayName("saveStreak — 조건 분기 + 1일 streak 복구 semantics")
    class SaveStreakTest {

        @Test
        @DisplayName("TC-5: 그저께 streakCount=1이면 어제(가상)=2로 복구된다 (1일 streak 복구 고정)")
        void saveStreak_one_day_recovery() {
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            LocalDate dayBefore = today.minusDays(2);

            when(accountLinkedUserResolver.resolveActiveUserBySocialId(SOCIAL_ID)).thenReturn(Optional.of(user));
            when(accountLinkRepository.existsByUserIdAndStatus(any(), any())).thenReturn(false);
            when(userAttendanceRepository.findByUserIdAndAttendanceDate(USER_ID, today)).thenReturn(Optional.empty());
            when(userAttendanceRepository.findByUserIdAndAttendanceDate(USER_ID, yesterday)).thenReturn(Optional.empty());
            when(userAttendanceRepository.findByUserIdAndAttendanceDate(USER_ID, dayBefore))
                    .thenReturn(Optional.of(stubAttendance(dayBefore, 1)));

            AttendanceDto.streakSaveResponse response =
                    attendanceService.saveStreak(SOCIAL_ID, "TOSS", "REWARDED");

            assertThat(response.getStreakCount()).isEqualTo(2);
            assertThat(response.getEarnedGrams()).isZero();

            ArgumentCaptor<UserAttendance> captor = ArgumentCaptor.forClass(UserAttendance.class);
            verify(userAttendanceRepository).saveAndFlush(captor.capture());
            UserAttendance saved = captor.getValue();
            assertThat(saved.getAttendanceDate()).isEqualTo(yesterday);
            assertThat(saved.getStreakCount()).isEqualTo(2);
            assertThat(saved.getStreakSaved()).isTrue();
        }

        @Test
        @DisplayName("TC-6: 오늘 이미 출석했으면 IllegalArgumentException")
        void saveStreak_reject_when_today_attended() {
            LocalDate today = LocalDate.now();
            when(accountLinkedUserResolver.resolveActiveUserBySocialId(SOCIAL_ID)).thenReturn(Optional.of(user));
            when(accountLinkRepository.existsByUserIdAndStatus(any(), any())).thenReturn(false);
            when(userAttendanceRepository.findByUserIdAndAttendanceDate(USER_ID, today))
                    .thenReturn(Optional.of(stubAttendance(today, 5)));

            assertThatThrownBy(() -> attendanceService.saveStreak(SOCIAL_ID, "TOSS", "REWARDED"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("오늘");

            verify(userAttendanceRepository, never()).saveAndFlush(any());
            verify(adWatchEventRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-7: 어제 이미 출석했으면 IllegalArgumentException")
        void saveStreak_reject_when_yesterday_attended() {
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            when(accountLinkedUserResolver.resolveActiveUserBySocialId(SOCIAL_ID)).thenReturn(Optional.of(user));
            when(accountLinkRepository.existsByUserIdAndStatus(any(), any())).thenReturn(false);
            when(userAttendanceRepository.findByUserIdAndAttendanceDate(USER_ID, today)).thenReturn(Optional.empty());
            when(userAttendanceRepository.findByUserIdAndAttendanceDate(USER_ID, yesterday))
                    .thenReturn(Optional.of(stubAttendance(yesterday, 3)));

            assertThatThrownBy(() -> attendanceService.saveStreak(SOCIAL_ID, "TOSS", "REWARDED"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("어제");
        }

        @Test
        @DisplayName("TC-8: 그저께 출석 없으면 IllegalArgumentException")
        void saveStreak_reject_when_no_day_before() {
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            LocalDate dayBefore = today.minusDays(2);
            when(accountLinkedUserResolver.resolveActiveUserBySocialId(SOCIAL_ID)).thenReturn(Optional.of(user));
            when(accountLinkRepository.existsByUserIdAndStatus(any(), any())).thenReturn(false);
            when(userAttendanceRepository.findByUserIdAndAttendanceDate(USER_ID, today)).thenReturn(Optional.empty());
            when(userAttendanceRepository.findByUserIdAndAttendanceDate(USER_ID, yesterday)).thenReturn(Optional.empty());
            when(userAttendanceRepository.findByUserIdAndAttendanceDate(USER_ID, dayBefore)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> attendanceService.saveStreak(SOCIAL_ID, "TOSS", "REWARDED"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("연속");
        }

        @Test
        @DisplayName("TC-9: saveStreak 성공 시 AdWatchEvent(ATTENDANCE_STREAK_SAVE)가 저장된다")
        void saveStreak_records_ad_watch_event() {
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            LocalDate dayBefore = today.minusDays(2);

            when(accountLinkedUserResolver.resolveActiveUserBySocialId(SOCIAL_ID)).thenReturn(Optional.of(user));
            when(accountLinkRepository.existsByUserIdAndStatus(any(), any())).thenReturn(false);
            when(userAttendanceRepository.findByUserIdAndAttendanceDate(USER_ID, today)).thenReturn(Optional.empty());
            when(userAttendanceRepository.findByUserIdAndAttendanceDate(USER_ID, yesterday)).thenReturn(Optional.empty());
            when(userAttendanceRepository.findByUserIdAndAttendanceDate(USER_ID, dayBefore))
                    .thenReturn(Optional.of(stubAttendance(dayBefore, 3)));

            attendanceService.saveStreak(SOCIAL_ID, "TOSS", "REWARDED");

            ArgumentCaptor<AdWatchEvent> captor = ArgumentCaptor.forClass(AdWatchEvent.class);
            verify(adWatchEventRepository).save(captor.capture());
            AdWatchEvent event = captor.getValue();
            assertThat(event.getWatchLocation()).isEqualTo(AdWatchLocation.ATTENDANCE_STREAK_SAVE);
            assertThat(event.getTossAdResponse()).isEqualTo("REWARDED");
            assertThat(event.getUserId()).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("grantAdBonus — 기존 RewardService 위임 + AdWatchEvent 기록")
    class GrantAdBonusTest {

        @Test
        @DisplayName("TC-10: grantAdBonus는 rewardService.grantAttendanceAdBonus 호출 결과의 earnedGrams를 반환한다")
        void grantAdBonus_delegates_to_reward_service() {
            when(rewardService.grantAttendanceAdBonus(SOCIAL_ID))
                    .thenReturn(RewardDto.rewardResponse.builder().earnedGrams(3).message("+3g").build());
            when(accountLinkedUserResolver.resolveActiveUserBySocialId(SOCIAL_ID)).thenReturn(Optional.of(user));

            AttendanceDto.adBonusResponse response =
                    attendanceService.grantAdBonus(SOCIAL_ID, "TOSS", "REWARDED");

            assertThat(response.getEarnedGrams()).isEqualTo(3);
            verify(rewardService).grantAttendanceAdBonus(SOCIAL_ID);
        }

        @Test
        @DisplayName("TC-11: grantAdBonus는 AdWatchEvent(ATTENDANCE_AD_BONUS, tossAdResponse=adFormat)를 저장한다")
        void grantAdBonus_records_ad_watch_event() {
            when(rewardService.grantAttendanceAdBonus(SOCIAL_ID))
                    .thenReturn(RewardDto.rewardResponse.builder().earnedGrams(5).message("+5g").build());
            when(accountLinkedUserResolver.resolveActiveUserBySocialId(SOCIAL_ID)).thenReturn(Optional.of(user));

            attendanceService.grantAdBonus(SOCIAL_ID, "TOSS", "INTERSTITIAL");

            ArgumentCaptor<AdWatchEvent> captor = ArgumentCaptor.forClass(AdWatchEvent.class);
            verify(adWatchEventRepository).save(captor.capture());
            AdWatchEvent event = captor.getValue();
            assertThat(event.getWatchLocation()).isEqualTo(AdWatchLocation.ATTENDANCE_AD_BONUS);
            assertThat(event.getTossAdResponse()).isEqualTo("INTERSTITIAL");
        }

        @Test
        @DisplayName("TC-12: adFormat=null 호출은 AdWatchEvent.tossAdResponse=\"UNKNOWN\"으로 저장한다")
        void grantAdBonus_unknown_format_when_null() {
            when(rewardService.grantAttendanceAdBonus(SOCIAL_ID))
                    .thenReturn(RewardDto.rewardResponse.builder().earnedGrams(2).message("+2g").build());
            when(accountLinkedUserResolver.resolveActiveUserBySocialId(SOCIAL_ID)).thenReturn(Optional.of(user));

            attendanceService.grantAdBonus(SOCIAL_ID, null, null);

            ArgumentCaptor<AdWatchEvent> captor = ArgumentCaptor.forClass(AdWatchEvent.class);
            verify(adWatchEventRepository).save(captor.capture());
            assertThat(captor.getValue().getTossAdResponse()).isEqualTo("UNKNOWN");
        }
    }

    // === helpers ===

    private static UserAttendance stubAttendance(LocalDate date, int streakCount) {
        UserAttendance ua = new UserAttendance(USER_ID, date, streakCount, false);
        return ua;
    }
}
