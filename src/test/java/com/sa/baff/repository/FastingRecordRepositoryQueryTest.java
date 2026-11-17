package com.sa.baff.repository;

import com.sa.baff.domain.FastingRecord;
import com.sa.baff.domain.Role;
import com.sa.baff.domain.UserB;
import com.sa.baff.domain.type.UserStatus;
import com.sa.baff.util.FastingEndStatus;
import com.sa.baff.util.FastingMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M-043 — 새 JPQL(진행 중 · 잠금 조회 · 겹침)을 실제 PostgreSQL 에서 확인한다.
 * 쿼리 문법 오류는 서버 기동 때 터지므로 배포 전에 한 번은 이 테스트로 본다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers
@EnabledIfSystemProperty(named = "test.docker", matches = "true",
        disabledReason = "Docker 미실행 환경에서는 건너뜁니다. 실행: ./gradlew test -Dtest.docker=true")
class FastingRecordRepositoryQueryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("baff_test").withUsername("baff").withPassword("baff");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired UserRepository userRepository;
    @Autowired FastingRecordRepository fastingRecordRepository;
    @Autowired TransactionTemplate transactionTemplate;

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 1, 20, 0);

    @AfterEach
    void cleanup() {
        fastingRecordRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void unknownIsNotActive_newActiveIsFound() {
        UserB user = saveUser();
        FastingRecord unknown = save(user, T0, null, FastingEndStatus.UNKNOWN);

        assertThat(fastingRecordRepository.findActive(user)).isEmpty();

        FastingRecord active = save(user, T0.plusDays(13), null, null);
        assertThat(fastingRecordRepository.findActive(user)).get()
                .extracting(FastingRecord::getId).isEqualTo(active.getId());
        assertThat(unknown.getId()).isNotEqualTo(active.getId());
    }

    @Test
    void overlap_detectsOnlyIntersectingEndedRecords() {
        UserB user = saveUser();
        FastingRecord ended = save(user, T0, T0.plusHours(16), FastingEndStatus.CONFIRMED);
        FastingRecord current = save(user, T0.plusHours(20), null, null);

        assertThat(fastingRecordRepository.existsOverlap(user, current.getId(), T0.plusHours(10), T0.plusHours(30))).isTrue();
        assertThat(fastingRecordRepository.existsOverlap(user, current.getId(), T0.plusHours(16), T0.plusHours(30))).isFalse();
        assertThat(fastingRecordRepository.existsOverlap(user, ended.getId(), T0, T0.plusHours(16))).isFalse();
    }

    @Test
    void findByIdForUpdate_readsInTransaction() {
        UserB user = saveUser();
        FastingRecord r = save(user, T0, null, null);

        Long found = transactionTemplate.execute(s -> fastingRecordRepository.findByIdForUpdate(r.getId()).orElseThrow().getId());

        assertThat(found).isEqualTo(r.getId());
    }

    private UserB saveUser() {
        UserB user = new UserB("fasting@test.com", "fasting", "img", "fasting_social", "toss", "TOSS");
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(Role.USER);
        return userRepository.save(user);
    }

    private FastingRecord save(UserB user, LocalDateTime start, LocalDateTime end, FastingEndStatus status) {
        FastingRecord r = FastingRecord.builder()
                .user(user).startTime(start).endTime(end).mode(FastingMode.SIXTEEN_EIGHT).targetHours(16).build();
        r.setEndStatus(status);
        return fastingRecordRepository.save(r);
    }
}
