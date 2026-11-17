package com.sa.baff.service;

import com.sa.baff.domain.FastingRecord;
import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.FastingRecordDto;
import com.sa.baff.model.vo.FastingRecordVO;
import com.sa.baff.repository.AiAnalysisRepository;
import com.sa.baff.repository.AiFeatureConfigRepository;
import com.sa.baff.repository.FastingRecordRepository;
import com.sa.baff.service.account.AccountLinkedUserResolver;
import com.sa.baff.util.DateTimeUtils;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** M-043 시작 · 진행 · 종료 (spec §5 · §7 수용기준 1 ~ 7 · 10 · 11) */
@ExtendWith(MockitoExtension.class)
class FastingRecordServiceImplEndTest {

    @Mock private FastingRecordRepository fastingRecordRepository;
    @Mock private AccountLinkedUserResolver accountLinkedUserResolver;
    @Mock private AiAnalysisRepository aiAnalysisRepository;
    @Mock private AiFeatureConfigRepository aiFeatureConfigRepository;
    @Mock private AnthropicApiClient anthropicApiClient;
    @Mock private RewardService rewardService;

    @InjectMocks private FastingRecordServiceImpl service;

    private static final String SOCIAL_ID = "toss_42";
    private static final Long RECORD_ID = 7L;
    private UserB user;

    @BeforeEach
    void setUp() throws Exception {
        user = new UserB("user@toss.im", "tester", "img", SOCIAL_ID, "toss", "TOSS");
        setId(user, UserB.class, 42L);
        lenient().when(accountLinkedUserResolver.resolveActiveUserBySocialId(SOCIAL_ID)).thenReturn(Optional.of(user));
    }

    // ── 종료 ──

    @Test
    @DisplayName("상한 안(20시간) — 식사 시각 없이 지금 종료 · 달성 · 실제 지급 그램을 응답")
    void end_now_within48h() {
        FastingRecord r = givenActive(DateTimeUtils.now().minusHours(20), 16);
        when(rewardService.tryGrantFastingCompleteReward(SOCIAL_ID, RECORD_ID)).thenReturn(1);

        FastingRecordDto.EndFastingResponse res = service.endFasting(endReq(null, null), SOCIAL_ID);

        assertThat(res.getActualMinutes()).isBetween(1199, 1200);
        assertThat(res.getCompleted()).isTrue();
        assertThat(res.getEarnedGrams()).isEqualTo(1);
        assertThat(res.getEndStatus()).isEqualTo(FastingEndStatus.CONFIRMED);
        assertThat(r.getEndStatus()).isEqualTo(FastingEndStatus.CONFIRMED);
    }

    @Test
    @DisplayName("13일 미접속 — 식사 시각 없이 종료하면 409 · 300시간대로 저장되지 않는다")
    void end_now_over48h_rejected() {
        FastingRecord r = givenActive(DateTimeUtils.now().minusDays(13), 16);

        assertThatThrownBy(() -> service.endFasting(endReq(null, null), SOCIAL_ID))
                .isInstanceOf(IllegalStateException.class).hasMessage("식사 시각을 입력해 주세요.");
        assertThat(r.getEndTime()).isNull();
        assertThat(r.getActualMinutes()).isNull();
        verify(fastingRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("13일 미접속 — 식사 시각 시작+16시간 30분으로 복구 → 990분 · 달성")
    void end_recover_1630() {
        LocalDateTime start = DateTimeUtils.now().minusDays(13).withSecond(0).withNano(0);
        givenActive(start, 16);
        when(rewardService.tryGrantFastingCompleteReward(SOCIAL_ID, RECORD_ID)).thenReturn(1);

        FastingRecordDto.EndFastingResponse res = service.endFasting(endReq(start.plusMinutes(990), null), SOCIAL_ID);

        assertThat(res.getActualMinutes()).isEqualTo(990);
        assertThat(res.getCompleted()).isTrue();
        assertThat(res.getEndStatus()).isEqualTo(FastingEndStatus.CONFIRMED);
    }

    @Test
    @DisplayName("오늘 보상을 이미 받았어도 복구는 저장되고 0g")
    void end_recover_rewardZero() {
        LocalDateTime start = DateTimeUtils.now().minusDays(2).withSecond(0).withNano(0);
        FastingRecord r = givenActive(start, 16);
        when(rewardService.tryGrantFastingCompleteReward(SOCIAL_ID, RECORD_ID)).thenReturn(0);

        FastingRecordDto.EndFastingResponse res = service.endFasting(endReq(start.plusHours(17), null), SOCIAL_ID);

        assertThat(res.getEarnedGrams()).isZero();
        assertThat(res.getCompleted()).isTrue();
        assertThat(r.getEndTime()).isEqualTo(start.plusHours(17));
        verify(fastingRecordRepository).save(r);
    }

    @Test
    @DisplayName("목표 미달 복구 — 보상을 부르지 않는다")
    void end_recover_notCompleted_noReward() {
        LocalDateTime start = DateTimeUtils.now().minusDays(3).withSecond(0).withNano(0);
        givenActive(start, 16);

        FastingRecordDto.EndFastingResponse res = service.endFasting(endReq(start.plusHours(10), null), SOCIAL_ID);

        assertThat(res.getCompleted()).isFalse();
        assertThat(res.getEarnedGrams()).isZero();
        verify(rewardService, never()).tryGrantFastingCompleteReward(anyString(), anyLong());
    }

    @Test
    @DisplayName("기억 안 나요 — UNKNOWN · 종료 시각 · 분 비움 · 보상 없음")
    void end_unknown() {
        FastingRecord r = givenActive(DateTimeUtils.now().minusDays(13), 16);

        FastingRecordDto.EndFastingResponse res = service.endFasting(endReq(null, true), SOCIAL_ID);

        assertThat(res.getEndStatus()).isEqualTo(FastingEndStatus.UNKNOWN);
        assertThat(res.getEarnedGrams()).isZero();
        assertThat(r.getEndStatus()).isEqualTo(FastingEndStatus.UNKNOWN);
        assertThat(r.getEndTime()).isNull();
        assertThat(r.getActualMinutes()).isNull();
        assertThat(r.getCompleted()).isFalse();
        verify(rewardService, never()).tryGrantFastingCompleteReward(anyString(), anyLong());
    }

    @Test
    @DisplayName("다른 기록과 겹치면 400")
    void end_overlap_rejected() {
        LocalDateTime start = DateTimeUtils.now().minusDays(3).withSecond(0).withNano(0);
        givenActive(start, 16);
        when(fastingRecordRepository.existsOverlap(any(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.endFasting(endReq(start.plusHours(16), null), SOCIAL_ID))
                .isInstanceOf(IllegalArgumentException.class);
        verify(fastingRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("상한을 넘는 식사 시각은 400")
    void end_recover_over48h_rejected() {
        LocalDateTime start = DateTimeUtils.now().minusDays(13).withSecond(0).withNano(0);
        givenActive(start, 16);

        assertThatThrownBy(() -> service.endFasting(endReq(start.plusMinutes(2881), null), SOCIAL_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("이미 종료(UNKNOWN 포함)된 기록은 409 — 두 번째 요청은 아무것도 하지 않는다")
    void end_alreadyEnded_rejected() {
        FastingRecord r = givenActive(DateTimeUtils.now().minusDays(13), 16);
        r.setEndStatus(FastingEndStatus.UNKNOWN);

        assertThatThrownBy(() -> service.endFasting(endReq(null, true), SOCIAL_ID))
                .isInstanceOf(IllegalStateException.class);
        verify(rewardService, never()).tryGrantFastingCompleteReward(anyString(), anyLong());
    }

    @Test
    @DisplayName("종료는 잠금 조회로 읽는다")
    void end_usesLock() {
        givenActive(DateTimeUtils.now().minusHours(5), 16);

        service.endFasting(endReq(null, null), SOCIAL_ID);

        verify(fastingRecordRepository).findByIdForUpdate(RECORD_ID);
        verify(fastingRecordRepository, never()).findById(any());
    }

    // ── 시작 · 진행 ──

    @Test
    @DisplayName("진행 중이 없으면(UNKNOWN 은 진행 중 아님 — 쿼리 조건) 바로 시작")
    void start_whenNoActive() throws Exception {
        when(fastingRecordRepository.findActive(user)).thenReturn(Optional.empty());
        when(fastingRecordRepository.save(any(FastingRecord.class))).thenAnswer(inv -> {
            FastingRecord saved = inv.getArgument(0);
            setId(saved, FastingRecord.class, 8L);
            return saved;
        });

        assertThat(service.startFasting(startReq(FastingMode.SIXTEEN_EIGHT, null), SOCIAL_ID).getId()).isEqualTo(8L);
    }

    @Test
    @DisplayName("사용자 지정 목표 37시간은 거절, 36시간은 시작")
    void start_customLimit() {
        assertThatThrownBy(() -> service.startFasting(startReq(FastingMode.CUSTOM, 37), SOCIAL_ID))
                .isInstanceOf(IllegalArgumentException.class);

        when(fastingRecordRepository.findActive(user)).thenReturn(Optional.empty());
        when(fastingRecordRepository.save(any(FastingRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        service.startFasting(startReq(FastingMode.CUSTOM, 36), SOCIAL_ID);
        verify(fastingRecordRepository).save(any(FastingRecord.class));
    }

    @Test
    @DisplayName("진행 조회 — 13일 지났으면 확인 필요 · 상한 48")
    void active_needsEndTime() throws Exception {
        FastingRecord r = record(DateTimeUtils.now().minusDays(13), 16);
        when(fastingRecordRepository.findActive(user)).thenReturn(Optional.of(r));

        FastingRecordDto.ActiveFasting dto = service.getActiveFasting(SOCIAL_ID);

        assertThat(dto.getNeedsEndTime()).isTrue();
        assertThat(dto.getMaxHours()).isEqualTo(48);
    }

    @Test
    @DisplayName("진행 조회 — 20시간이면 확인 필요 아님")
    void active_within48h() throws Exception {
        when(fastingRecordRepository.findActive(user)).thenReturn(Optional.of(record(DateTimeUtils.now().minusHours(20), 16)));

        assertThat(service.getActiveFasting(SOCIAL_ID).getNeedsEndTime()).isFalse();
    }

    // ── 도우미 ──

    private FastingRecord givenActive(LocalDateTime start, int targetHours) {
        try {
            FastingRecord r = record(start, targetHours);
            when(fastingRecordRepository.findByIdForUpdate(RECORD_ID)).thenReturn(Optional.of(r));
            return r;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private FastingRecord record(LocalDateTime start, int targetHours) throws Exception {
        FastingRecord r = FastingRecord.builder()
                .user(user).startTime(start).mode(FastingMode.SIXTEEN_EIGHT).targetHours(targetHours).build();
        setId(r, FastingRecord.class, RECORD_ID);
        return r;
    }

    private FastingRecordVO.EndFasting endReq(LocalDateTime endTime, Boolean unknown) {
        FastingRecordVO.EndFasting req = new FastingRecordVO.EndFasting();
        req.setFastingRecordId(RECORD_ID);
        req.setEndTime(endTime);
        req.setUnknown(unknown);
        return req;
    }

    private FastingRecordVO.StartFasting startReq(FastingMode mode, Integer hours) {
        FastingRecordVO.StartFasting req = new FastingRecordVO.StartFasting();
        req.setMode(mode);
        req.setTargetHours(hours);
        return req;
    }

    private static void setId(Object target, Class<?> type, Long id) throws Exception {
        Field f = type.getDeclaredField("id");
        f.setAccessible(true);
        f.set(target, id);
    }
}
