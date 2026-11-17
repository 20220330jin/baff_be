package com.sa.baff.service;

import com.sa.baff.domain.FastingRecord;
import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.FastingRecordDto;
import com.sa.baff.repository.AiAnalysisRepository;
import com.sa.baff.repository.AiFeatureConfigRepository;
import com.sa.baff.repository.FastingRecordRepository;
import com.sa.baff.service.account.AccountLinkedUserResolver;
import com.sa.baff.util.FastingEndStatus;
import com.sa.baff.util.FastingMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** M-043 통계 제외 (spec §7 수용기준 3 · 8 · 13) */
@ExtendWith(MockitoExtension.class)
class FastingRecordServiceImplListTest {

    @Mock private FastingRecordRepository fastingRecordRepository;
    @Mock private AccountLinkedUserResolver accountLinkedUserResolver;
    @Mock private AiAnalysisRepository aiAnalysisRepository;
    @Mock private AiFeatureConfigRepository aiFeatureConfigRepository;
    @Mock private AnthropicApiClient anthropicApiClient;
    @Mock private RewardService rewardService;

    @InjectMocks private FastingRecordServiceImpl service;

    private static final String SOCIAL_ID = "toss_42";
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 1, 20, 0);
    private UserB user;
    private long nextId = 1;

    @BeforeEach
    void setUp() throws Exception {
        user = new UserB("user@toss.im", "tester", "img", SOCIAL_ID, "toss", "TOSS");
        Field f = UserB.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(user, 42L);
        when(accountLinkedUserResolver.resolveActiveUserBySocialId(SOCIAL_ID)).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("통계는 확인된 종료 기록만 — 확인 필요 · 미확인은 빼고, 목록에는 모두 상태와 함께 내려준다")
    void list_excludesUnconfirmed() throws Exception {
        // 최신순 (저장소가 startTime 내림차순으로 준다)
        FastingRecord active = rec(T0.plusDays(20), null, null, false, null);
        FastingRecord unknown = rec(T0.plusDays(15), null, null, false, FastingEndStatus.UNKNOWN);
        FastingRecord ok20h = rec(T0.plusDays(3), T0.plusDays(3).plusHours(20), 1200, true, null);       // 마이그레이션 전 (상태 null)
        FastingRecord needsCheck = rec(T0.plusDays(2), T0.plusDays(2).plusHours(319), 19140, true, FastingEndStatus.NEEDS_CHECK);
        FastingRecord ok10h = rec(T0, T0.plusHours(10), 600, false, FastingEndStatus.CONFIRMED);
        when(fastingRecordRepository.findByUserAndDelYnOrderByStartTimeDesc(user, 'N'))
                .thenReturn(List.of(active, unknown, ok20h, needsCheck, ok10h));

        FastingRecordDto.GetFastingList data = service.getFastingList(SOCIAL_ID);

        assertThat(data.getTotalRecords()).isEqualTo(3);        // 진행 중 1 + 확인된 종료 2
        assertThat(data.getCompletedRecords()).isEqualTo(1);
        assertThat(data.getTotalFastingMinutes()).isEqualTo(1800);
        assertThat(data.getAverageFastingMinutes()).isEqualTo(900.0);
        assertThat(data.getCurrentStreak()).isEqualTo(1);       // 20h 달성 → 10h 미달에서 끊김 (319h 는 건너뜀)
        assertThat(data.getRecords()).hasSize(5);
        assertThat(data.getRecords()).extracting(FastingRecordDto.FastingRecordResponse::getEndStatus)
                .containsExactly(null, FastingEndStatus.UNKNOWN, FastingEndStatus.CONFIRMED,
                        FastingEndStatus.NEEDS_CHECK, FastingEndStatus.CONFIRMED);
    }

    @Test
    @DisplayName("연속 달성은 확인 필요 기록을 건너뛰고 센다")
    void streak_skipsExcluded() throws Exception {
        FastingRecord a = rec(T0.plusDays(3), T0.plusDays(3).plusHours(17), 1020, true, FastingEndStatus.CONFIRMED);
        FastingRecord b = rec(T0.plusDays(2), T0.plusDays(2).plusHours(100), 6000, true, FastingEndStatus.NEEDS_CHECK);
        FastingRecord c = rec(T0, T0.plusHours(16), 960, true, FastingEndStatus.CONFIRMED);
        when(fastingRecordRepository.findByUserAndDelYnOrderByStartTimeDesc(user, 'N')).thenReturn(List.of(a, b, c));

        assertThat(service.getFastingList(SOCIAL_ID).getCurrentStreak()).isEqualTo(2);
    }

    private FastingRecord rec(LocalDateTime start, LocalDateTime end, Integer minutes, boolean completed,
                              FastingEndStatus status) throws Exception {
        FastingRecord r = FastingRecord.builder().user(user).startTime(start).endTime(end)
                .mode(FastingMode.SIXTEEN_EIGHT).targetHours(16).actualMinutes(minutes).completed(completed).build();
        r.setEndStatus(status);
        Field f = FastingRecord.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(r, nextId++);
        return r;
    }
}
