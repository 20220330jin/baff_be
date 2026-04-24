package com.sa.baff.service;

import com.sa.baff.domain.Role;
import com.sa.baff.domain.UserAttendance;
import com.sa.baff.domain.UserB;
import com.sa.baff.domain.type.AttendanceSource;
import com.sa.baff.domain.type.UserStatus;
import com.sa.baff.repository.AdWatchEventRepository;
import com.sa.baff.repository.PieceRepository;
import com.sa.baff.repository.UserAttendanceRepository;
import com.sa.baff.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S7-14 BE-8b — streak-save 동시성 유니크 충돌 → 409 검증 (integration test).
 *
 * `UserAttendance`의 (user_id, attendance_date) UNIQUE 제약을 실제 DB에서 건드려
 * `DataIntegrityViolationException`이 발생함을 확인. GlobalExceptionHandler 매핑은
 * 웹 레이어 테스트이므로 본 테스트는 예외 타입까지만 검증한다 (controller E2E는 수동/실기기).
 *
 * TransactionTemplate + CountDownLatch + 두 스레드로 flush 타이밍을 맞춘다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers
@EnabledIfSystemProperty(named = "test.docker", matches = "true",
        disabledReason = "Docker 미실행 환경에서는 건너뜁니다. 실행: ./gradlew test -Dtest.docker=true")
class AttendanceStreakSaveConcurrencyTest {

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

    @Autowired UserRepository userRepository;
    @Autowired UserAttendanceRepository userAttendanceRepository;
    @Autowired PieceRepository pieceRepository;
    @Autowired AdWatchEventRepository adWatchEventRepository;
    @Autowired TransactionTemplate transactionTemplate;

    @AfterEach
    void cleanup() {
        adWatchEventRepository.deleteAll();
        userAttendanceRepository.deleteAll();
        pieceRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * 두 스레드가 동시에 동일 userId + yesterday 날짜로 UserAttendance를 flush하면
     * 하나는 성공, 하나는 DataIntegrityViolationException을 던진다.
     */
    @Test
    void concurrent_streak_save_raises_data_integrity_violation() throws Exception {
        UserB user = saveUser("concurrent@test.com", "concurrent_social", "toss");
        Long userId = user.getId();
        LocalDate yesterday = LocalDate.now().minusDays(1);

        TransactionTemplate requiresNew = new TransactionTemplate(transactionTemplate.getTransactionManager());
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch readyGate = new CountDownLatch(2);
        CountDownLatch doneGate = new CountDownLatch(2);
        AtomicReference<Throwable> errorA = new AtomicReference<>();
        AtomicReference<Throwable> errorB = new AtomicReference<>();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            pool.submit(() -> {
                try {
                    requiresNew.executeWithoutResult(status -> {
                        readyGate.countDown();
                        try {
                            startGate.await();
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        UserAttendance ua = new UserAttendance(userId, yesterday, 2, true);
                        ua.setSource(AttendanceSource.TOSS);
                        userAttendanceRepository.saveAndFlush(ua);
                    });
                } catch (Throwable t) {
                    errorA.set(rootCause(t));
                } finally {
                    doneGate.countDown();
                }
            });

            pool.submit(() -> {
                try {
                    requiresNew.executeWithoutResult(status -> {
                        readyGate.countDown();
                        try {
                            startGate.await();
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        UserAttendance ua = new UserAttendance(userId, yesterday, 2, true);
                        ua.setSource(AttendanceSource.TOSS);
                        userAttendanceRepository.saveAndFlush(ua);
                    });
                } catch (Throwable t) {
                    errorB.set(rootCause(t));
                } finally {
                    doneGate.countDown();
                }
            });

            readyGate.await(5, TimeUnit.SECONDS);
            startGate.countDown();
            doneGate.await(10, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        // 하나는 성공(예외 없음), 하나는 DataIntegrityViolationException
        long failures = countDataIntegrityFailures(errorA.get(), errorB.get());
        long successes = 2 - failures;

        assertThat(failures).as("두 스레드 중 하나는 유니크 충돌로 실패해야 한다").isEqualTo(1L);
        assertThat(successes).as("두 스레드 중 하나는 성공해야 한다").isEqualTo(1L);

        assertThat(userAttendanceRepository.findByUserIdAndAttendanceDate(userId, yesterday))
                .as("성공한 한 건만 DB에 남는다")
                .isPresent();
    }

    private long countDataIntegrityFailures(Throwable... errors) {
        long count = 0;
        for (Throwable t : errors) {
            if (t instanceof DataIntegrityViolationException) {
                count++;
            }
        }
        return count;
    }

    private Throwable rootCause(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof DataIntegrityViolationException) {
                return cur;
            }
            if (cur.getCause() == null || cur.getCause() == cur) {
                break;
            }
            cur = cur.getCause();
        }
        return t;
    }

    private UserB saveUser(String email, String socialId, String provider) {
        UserB user = new UserB(email, email.split("@")[0], "img", socialId, provider, "TOSS");
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(Role.USER);
        return userRepository.save(user);
    }
}
