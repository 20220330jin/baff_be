package com.sa.baff.service;

import com.sa.baff.domain.SmartPushConfig;
import com.sa.baff.domain.SmartPushHistory;
import com.sa.baff.model.SmartPushRecipient;
import com.sa.baff.repository.SmartPushConfigRepository;
import com.sa.baff.repository.SmartPushHistoryRepository;
import com.sa.baff.service.notification.TossMessageApiClient;
import com.sa.baff.util.SmartPushTargetStrategy;
import com.sa.baff.util.SmartPushType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 스마트발송 서비스 (spec §3).
 *
 * CP1 Round 2 P0 반영:
 * - 발송 대상을 List<SmartPushRecipient>로 반환 (병합 Primary 커버)
 * - executePush에서 recipient.tossUserKey()로 발송, recipient.userId()로 이력 저장
 * - buildContext 훅 (현재 빈 Map)
 *
 * CP2 P1/P2 반영:
 * - 중복 방지는 success=true 이력만 기준 → 실패 이력은 당일 재시도 허용
 * - 날짜 경계는 Asia/Seoul TZ 고정 (스케줄러 zone과 일치)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SmartPushServiceImpl implements SmartPushService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final SmartPushConfigRepository configRepository;
    private final SmartPushHistoryRepository historyRepository;
    private final TossMessageApiClient tossMessageApiClient;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 토스 발송 가능 유저 전체 (spec §3.2).
     *
     * 2경로 UNION:
     *   1) 직접 토스 가입자: users.provider='toss' OR platform='TOSS'
     *   2) 병합 Primary: account_links.provider='toss' AND status='ACTIVE' JOIN users.status='ACTIVE'
     *
     * userId 기준 중복 제거는 구조상 발생 불가 (병합 시 secondary는 MERGED 상태로 제외됨).
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<SmartPushRecipient> findAllTossRecipients() {
        String sql = """
                SELECT u."userId" AS user_id, u.social_id AS toss_user_key
                  FROM users u
                 WHERE u."delYn" = 'N'
                   AND u.status = 'ACTIVE'
                   AND u.social_id IS NOT NULL
                   AND (u.provider = 'toss' OR u.platform = 'TOSS')
                UNION
                SELECT u."userId" AS user_id, al.provider_user_id AS toss_user_key
                  FROM users u
                  JOIN account_links al ON al.user_id = u."userId"
                 WHERE u."delYn" = 'N'
                   AND u.status = 'ACTIVE'
                   AND al.provider = 'toss'
                   AND al.status = 'ACTIVE'
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
        return rows.stream().map(r -> new SmartPushRecipient(
                ((Number) r[0]).longValue(),
                (String) r[1]
        )).toList();
    }

    /**
     * 최근 7일 활동 + 오늘 미출석 Recipient (spec §3.2).
     *
     * 기존 findAttendanceReminderTargets 로직 + findAllTossRecipients()와 같은 2경로 UNION 구조.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<SmartPushRecipient> findAttendanceReminderRecipients() {
        LocalDate today = LocalDate.now(SEOUL_ZONE);
        LocalDate weekAgo = today.minusDays(7);
        LocalDate yesterday = today.minusDays(1);

        String sql = """
                WITH candidates AS (
                    -- 경로 1: 직접 토스 가입자
                    SELECT u."userId" AS user_id, u.social_id AS toss_user_key
                      FROM users u
                     WHERE u."delYn" = 'N'
                       AND u.status = 'ACTIVE'
                       AND u.social_id IS NOT NULL
                       AND (u.provider = 'toss' OR u.platform = 'TOSS')
                    UNION
                    -- 경로 2: 병합 Primary
                    SELECT u."userId" AS user_id, al.provider_user_id AS toss_user_key
                      FROM users u
                      JOIN account_links al ON al.user_id = u."userId"
                     WHERE u."delYn" = 'N'
                       AND u.status = 'ACTIVE'
                       AND al.provider = 'toss'
                       AND al.status = 'ACTIVE'
                )
                SELECT c.user_id, c.toss_user_key
                  FROM candidates c
                 WHERE EXISTS (
                           SELECT 1 FROM user_attendances ua
                            WHERE ua.user_id = c.user_id
                              AND ua.attendance_date BETWEEN :weekAgo AND :yesterday
                       )
                   AND NOT EXISTS (
                           SELECT 1 FROM user_attendances ua
                            WHERE ua.user_id = c.user_id
                              AND ua.attendance_date = :today
                       )
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("weekAgo", weekAgo)
                .setParameter("yesterday", yesterday)
                .setParameter("today", today)
                .getResultList();
        return rows.stream().map(r -> new SmartPushRecipient(
                ((Number) r[0]).longValue(),
                (String) r[1]
        )).toList();
    }

    /**
     * 그램 100g 이상 보유 Recipient (spec §3.2).
     *
     * 기존 findExchangeReminderTargets 로직 + 2경로 UNION 구조.
     * thresholdDays 파라미터는 향후 "마지막 환전 후 N일 경과" 조건 확장용으로 보존 (현재 미사용).
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<SmartPushRecipient> findExchangeReminderRecipients(int thresholdDays) {
        String sql = """
                WITH candidates AS (
                    SELECT u."userId" AS user_id, u.social_id AS toss_user_key
                      FROM users u
                     WHERE u."delYn" = 'N'
                       AND u.status = 'ACTIVE'
                       AND u.social_id IS NOT NULL
                       AND (u.provider = 'toss' OR u.platform = 'TOSS')
                    UNION
                    SELECT u."userId" AS user_id, al.provider_user_id AS toss_user_key
                      FROM users u
                      JOIN account_links al ON al.user_id = u."userId"
                     WHERE u."delYn" = 'N'
                       AND u.status = 'ACTIVE'
                       AND al.provider = 'toss'
                       AND al.status = 'ACTIVE'
                )
                SELECT c.user_id, c.toss_user_key
                  FROM candidates c
                  JOIN pieces p ON p.user_id = c.user_id
                 WHERE p.balance >= 100
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
        return rows.stream().map(r -> new SmartPushRecipient(
                ((Number) r[0]).longValue(),
                (String) r[1]
        )).toList();
    }

    /**
     * 스마트발송 실행 (spec §3.5).
     *
     * 가드 순서:
     *   1) config 존재 확인
     *   2) enabled 확인
     *   3) templateCode blank 확인 (CP1 G4 가드 1)
     *   4) Recipient 루프에서 일일 중복 방지 (CP1 Round 2 §2)
     */
    @Override
    public void executePush(SmartPushType pushType) {
        Optional<SmartPushConfig> configOpt = configRepository.findByPushType(pushType);
        if (configOpt.isEmpty()) {
            log.info("[SmartPush] 설정 없음: {}", pushType);
            return;
        }

        SmartPushConfig config = configOpt.get();
        if (!config.getEnabled()) {
            log.info("[SmartPush] 비활성: {}", pushType);
            return;
        }

        if (config.getTemplateCode() == null || config.getTemplateCode().isBlank()) {
            log.warn("[SmartPush] templateCode 미입력으로 skip: type={}", pushType);
            return;
        }

        SmartPushTargetStrategy strategy = config.getTargetStrategy() != null
                ? config.getTargetStrategy()
                : SmartPushTargetStrategy.ALL_TOSS_USERS;

        List<SmartPushRecipient> recipients = switch (strategy) {
            case ALL_TOSS_USERS -> findAllTossRecipients();
            case ACTIVE_LAST_7_DAYS_NOT_ATTENDED -> findAttendanceReminderRecipients();
            case BALANCE_OVER_100G -> findExchangeReminderRecipients(
                    config.getThresholdDays() != null ? config.getThresholdDays() : 7);
        };

        log.info("[SmartPush] 발송 대상: type={}, strategy={}, count={}",
                pushType, strategy, recipients.size());

        LocalDateTime todayStart = LocalDate.now(SEOUL_ZONE).atStartOfDay();
        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;

        for (SmartPushRecipient recipient : recipients) {
            // 일일 중복 방지 가드 (CP2 P1 반영): 성공 이력만 기준 → 실패는 당일 재시도 가능
            if (historyRepository.existsByUserIdAndPushTypeAndSuccessAndRegDateTimeAfter(
                    recipient.userId(), pushType, true, todayStart)) {
                skipCount++;
                continue;
            }

            Map<String, String> context = buildContext(pushType, recipient.userId());
            TossMessageApiClient.SendResult result = tossMessageApiClient
                    .sendMessageWithDetail(recipient.tossUserKey(), config.getTemplateCode(), context);

            String apiResponse = result.success()
                    ? "SUCCESS"
                    : String.format("FAIL: %s / %s", result.errorCode(), result.errorReason());

            historyRepository.save(new SmartPushHistory(
                    recipient.userId(), pushType, apiResponse, result.success()));

            if (result.success()) successCount++;
            else failCount++;
        }

        log.info("[SmartPush] 발송 완료: type={}, total={}, success={}, fail={}, skip={}",
                pushType, recipients.size(), successCount, failCount, skipCount);
    }

    /**
     * 템플릿 변수 context 구성 (spec §3.5 — CP1 Round 2 §2 권장 훅).
     *
     * 현재는 빈 Map. 추후 개인화 메시지(N번째 출석, 보유 그램 등) 도입 시 분기.
     */
    private Map<String, String> buildContext(SmartPushType pushType, Long userId) {
        return Map.of();
    }
}
