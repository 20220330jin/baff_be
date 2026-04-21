package com.sa.baff.service;

import com.sa.baff.domain.AccountLink;
import com.sa.baff.domain.Piece;
import com.sa.baff.domain.Role;
import com.sa.baff.domain.SmartPushConfig;
import com.sa.baff.domain.SmartPushHistory;
import com.sa.baff.domain.UserAttendance;
import com.sa.baff.domain.UserB;
import com.sa.baff.domain.type.AccountLinkStatus;
import com.sa.baff.domain.type.UserStatus;
import com.sa.baff.model.SmartPushRecipient;
import com.sa.baff.repository.AccountLinkRepository;
import com.sa.baff.repository.PieceRepository;
import com.sa.baff.repository.SmartPushConfigRepository;
import com.sa.baff.repository.SmartPushHistoryRepository;
import com.sa.baff.repository.UserAttendanceRepository;
import com.sa.baff.repository.UserRepository;
import com.sa.baff.service.notification.TossMessageApiClient;
import com.sa.baff.util.SmartPushTargetStrategy;
import com.sa.baff.util.SmartPushType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SmartPushServiceImpl 네이티브 SQL + 실호출 플로우 검증 (spec §5 / CP2 P1).
 *
 * 검증 케이스:
 *   - 직접 토스 유저 / 병합 Primary 모두 findAllTossRecipients에 포함
 *   - status=MERGED 또는 delYn='Y' 유저는 제외
 *   - 카카오 유저(AccountLink 없음)는 제외
 *   - findAttendanceReminderRecipients: 최근 7일 출석 + 오늘 미출석
 *   - findExchangeReminderRecipients: balance >= 100g
 *   - executePush: 성공 이력만 중복 방지, 실패 이력은 당일 재시도 허용 (CP2 P1)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers
class SmartPushServiceImplNativeSqlTest {

    // 서비스와 동일 TZ 기준으로 날짜 계산 (CP2 Round 2 P2 반영)
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

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

    @Autowired SmartPushService smartPushService;
    @Autowired UserRepository userRepository;
    @Autowired AccountLinkRepository accountLinkRepository;
    @Autowired PieceRepository pieceRepository;
    @Autowired UserAttendanceRepository userAttendanceRepository;
    @Autowired SmartPushConfigRepository smartPushConfigRepository;
    @Autowired SmartPushHistoryRepository smartPushHistoryRepository;

    @MockitoBean TossMessageApiClient tossMessageApiClient;

    @BeforeEach
    void resetMocks() {
        when(tossMessageApiClient.sendMessageWithDetail(anyString(), anyString(), any()))
                .thenReturn(new TossMessageApiClient.SendResult(true, "SUCCESS", null, null));
    }

    @AfterEach
    void cleanup() {
        smartPushHistoryRepository.deleteAll();
        smartPushConfigRepository.deleteAll();
        userAttendanceRepository.deleteAll();
        pieceRepository.deleteAll();
        accountLinkRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ========== findAllTossRecipients ==========

    @Test
    void findAllTossRecipients_includesDirectTossUser() {
        UserB toss = saveUser("t1@test.com", "toss_userkey_111", "toss");

        List<SmartPushRecipient> recipients = smartPushService.findAllTossRecipients();

        assertThat(recipients).extracting(SmartPushRecipient::userId).contains(toss.getId());
        assertThat(recipients).extracting(SmartPushRecipient::tossUserKey).contains("toss_userkey_111");
    }

    @Test
    void findAllTossRecipients_includesMergedPrimaryViaAccountLink() {
        // Primary = 카카오 유저, account_links에 토스 연결
        UserB primary = saveUser("k1@test.com", "kakao_111", "kakao");
        saveAccountLink(primary.getId(), "toss", "toss_merged_key_999", AccountLinkStatus.ACTIVE);

        List<SmartPushRecipient> recipients = smartPushService.findAllTossRecipients();

        assertThat(recipients).extracting(SmartPushRecipient::userId).contains(primary.getId());
        assertThat(recipients).extracting(SmartPushRecipient::tossUserKey).contains("toss_merged_key_999");
    }

    @Test
    void findAllTossRecipients_excludesKakaoUserWithoutAccountLink() {
        UserB kakao = saveUser("k2@test.com", "kakao_222", "kakao");

        List<SmartPushRecipient> recipients = smartPushService.findAllTossRecipients();

        assertThat(recipients).extracting(SmartPushRecipient::userId).doesNotContain(kakao.getId());
    }

    @Test
    void findAllTossRecipients_excludesMergedStatus() {
        UserB merged = saveUser("m1@test.com", "toss_merged_secondary", "toss");
        merged.setStatus(UserStatus.MERGED);
        userRepository.save(merged);

        List<SmartPushRecipient> recipients = smartPushService.findAllTossRecipients();

        assertThat(recipients).extracting(SmartPushRecipient::userId).doesNotContain(merged.getId());
    }

    @Test
    void findAllTossRecipients_excludesRevokedAccountLink() {
        UserB primary = saveUser("k3@test.com", "kakao_333", "kakao");
        saveAccountLink(primary.getId(), "toss", "toss_revoked_key", AccountLinkStatus.REVOKED);

        List<SmartPushRecipient> recipients = smartPushService.findAllTossRecipients();

        assertThat(recipients).extracting(SmartPushRecipient::userId).doesNotContain(primary.getId());
    }

    // ========== findAttendanceReminderRecipients ==========

    @Test
    void findAttendanceReminderRecipients_includesRecentActiveNotAttendedToday() {
        UserB toss = saveUser("a1@test.com", "toss_active_key", "toss");
        saveAttendance(toss.getId(), LocalDate.now(SEOUL_ZONE).minusDays(2));  // 최근 7일 내 활동

        List<SmartPushRecipient> recipients = smartPushService.findAttendanceReminderRecipients();

        assertThat(recipients).extracting(SmartPushRecipient::userId).contains(toss.getId());
    }

    @Test
    void findAttendanceReminderRecipients_excludesTodayAttended() {
        UserB toss = saveUser("a2@test.com", "toss_today_attended", "toss");
        saveAttendance(toss.getId(), LocalDate.now(SEOUL_ZONE).minusDays(2));
        saveAttendance(toss.getId(), LocalDate.now(SEOUL_ZONE));  // 오늘 출석

        List<SmartPushRecipient> recipients = smartPushService.findAttendanceReminderRecipients();

        assertThat(recipients).extracting(SmartPushRecipient::userId).doesNotContain(toss.getId());
    }

    // ========== findExchangeReminderRecipients ==========

    @Test
    void findExchangeReminderRecipients_includesBalance100OrMore() {
        UserB toss = saveUser("e1@test.com", "toss_rich_key", "toss");
        savePiece(toss, 150L);

        List<SmartPushRecipient> recipients = smartPushService.findExchangeReminderRecipients(7);

        assertThat(recipients).extracting(SmartPushRecipient::userId).contains(toss.getId());
    }

    @Test
    void findExchangeReminderRecipients_excludesBalanceBelow100() {
        UserB toss = saveUser("e2@test.com", "toss_poor_key", "toss");
        savePiece(toss, 50L);

        List<SmartPushRecipient> recipients = smartPushService.findExchangeReminderRecipients(7);

        assertThat(recipients).extracting(SmartPushRecipient::userId).doesNotContain(toss.getId());
    }

    // ========== executePush: 성공 이력만 중복 방지 (CP2 P1) ==========

    @Test
    void executePush_skipsUserWithSuccessToday_retriesFailure() {
        UserB toss = saveUser("p1@test.com", "toss_retry_key", "toss");
        saveConfig(SmartPushType.ATTENDANCE_REMINDER, true, "test-template",
                SmartPushTargetStrategy.ALL_TOSS_USERS);

        // 이미 오늘 실패 이력 있음 → 재시도 허용되어야 함
        SmartPushHistory failedHistory = new SmartPushHistory(
                toss.getId(), SmartPushType.ATTENDANCE_REMINDER, "FAIL: SERVER_ERROR", false);
        smartPushHistoryRepository.save(failedHistory);

        smartPushService.executePush(SmartPushType.ATTENDANCE_REMINDER);

        // tossMessageApiClient가 호출되어야 함 (실패 이력은 skip 대상 아님)
        verify(tossMessageApiClient, times(1))
                .sendMessageWithDetail(eq("toss_retry_key"), eq("test-template"), any());
    }

    @Test
    void executePush_skipsUserWithSuccessToday() {
        UserB toss = saveUser("p2@test.com", "toss_already_sent_key", "toss");
        saveConfig(SmartPushType.ATTENDANCE_REMINDER, true, "test-template",
                SmartPushTargetStrategy.ALL_TOSS_USERS);

        // 이미 오늘 성공 이력 있음 → skip
        SmartPushHistory successHistory = new SmartPushHistory(
                toss.getId(), SmartPushType.ATTENDANCE_REMINDER, "SUCCESS", true);
        smartPushHistoryRepository.save(successHistory);

        smartPushService.executePush(SmartPushType.ATTENDANCE_REMINDER);

        // tossMessageApiClient가 호출되지 않아야 함
        verify(tossMessageApiClient, times(0))
                .sendMessageWithDetail(eq("toss_already_sent_key"), anyString(), any());
    }

    @Test
    void executePush_skipsWhenTemplateCodeBlank() {
        UserB toss = saveUser("p3@test.com", "toss_blank_template_key", "toss");
        saveConfig(SmartPushType.ATTENDANCE_REMINDER, true, null,
                SmartPushTargetStrategy.ALL_TOSS_USERS);  // templateCode 없음

        smartPushService.executePush(SmartPushType.ATTENDANCE_REMINDER);

        verify(tossMessageApiClient, times(0)).sendMessageWithDetail(anyString(), anyString(), any());
    }

    // ========== 헬퍼 ==========

    private UserB saveUser(String email, String socialId, String provider) {
        UserB user = new UserB(email, "테스트", null, socialId, provider, null);
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(Role.USER);
        user.setNickname("nick_" + socialId);
        return userRepository.save(user);
    }

    private void saveAccountLink(Long userId, String provider, String providerUserId, AccountLinkStatus status) {
        AccountLink link = new AccountLink(userId, provider, providerUserId);
        if (status == AccountLinkStatus.REVOKED) {
            link.revoke();
        }
        accountLinkRepository.save(link);
    }

    private void savePiece(UserB user, long balance) {
        Piece piece = new Piece(user);
        piece.setBalance(balance);
        pieceRepository.save(piece);
    }

    private void saveAttendance(Long userId, LocalDate date) {
        UserAttendance a = new UserAttendance(userId, date, 1, false);
        userAttendanceRepository.save(a);
    }

    private void saveConfig(SmartPushType type, boolean enabled, String templateCode,
                            SmartPushTargetStrategy strategy) {
        SmartPushConfig config = new SmartPushConfig();
        config.setPushType(type);
        config.setEnabled(enabled);
        config.setTemplateCode(templateCode);
        config.setTargetStrategy(strategy);
        smartPushConfigRepository.save(config);
    }
}
