package com.sa.baff.config;

import com.sa.baff.util.PieceTransactionType;
import com.sa.baff.util.RewardStatus;
import com.sa.baff.util.RewardType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * PostgreSQL enum 컬럼 CHECK 제약 자동 갱신.
 *
 * Hibernate ddl-auto=update는 컬럼/테이블 추가는 하지만 기존 enum CHECK 제약(`xxx_check`)에
 * 신규 enum 값을 반영하지 않아, 신규 enum row INSERT 시 ConstraintViolation 발생.
 * 이 runner가 부팅 시 알려진 enum 컬럼 CHECK 제약을 DROP + RECREATE 하여 항상 현재 Java enum과 동기화.
 *
 * Initializer들(101~103)보다 먼저 실행되도록 Order(50).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(50)
public class EnumCheckConstraintFixer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        fixCheck("reward_configs", "reward_type", RewardType.values());
        fixCheck("reward_histories", "reward_type", RewardType.values());
        fixCheck("reward_histories", "status", RewardStatus.values());
        fixCheck("piece_transactions", "type", PieceTransactionType.values());

        // 광고전략 — ad_position_code는 enum이 아닌 String varchar (OTHER 등 임의값 허용).
        // ddl-auto가 옛 enum 시절 흔적/캐시로 CHECK를 재생성하는 케이스 방어 — DROP only.
        dropCheck("ad_metric_banner_entry", "ad_position_code");
        dropCheck("ad_metric_image_entry", "ad_position_code");
    }

    /** 컬럼에 묶인 CHECK 제약 모두 DROP (varchar 자유값 허용용) */
    private void dropCheck(String table, String column) {
        try {
            jdbcTemplate.query(
                    "SELECT c.conname FROM pg_constraint c " +
                            "JOIN pg_class t ON t.oid = c.conrelid " +
                            "JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey) " +
                            "WHERE c.contype = 'c' AND t.relname = ? AND a.attname = ?",
                    rs -> {
                        String name = rs.getString(1);
                        try {
                            jdbcTemplate.execute("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + name);
                            log.info("[EnumCheckFix] {}.{} CHECK '{}' DROP 완료", table, column, name);
                        } catch (Exception ex) {
                            log.warn("[EnumCheckFix] {}.{} CHECK '{}' DROP 실패: {}", table, column, name, ex.getMessage());
                        }
                    },
                    table, column);
        } catch (Exception e) {
            log.warn("[EnumCheckFix] {}.{} CHECK 조회 실패 (무시): {}", table, column, e.getMessage());
        }
    }

    private void fixCheck(String table, String column, Enum<?>[] values) {
        String constraintName = table + "_" + column + "_check";
        String inClause = Arrays.stream(values)
                .map(Enum::name)
                .collect(Collectors.joining("','", "'", "'"));
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + constraintName);
            jdbcTemplate.execute(
                    "ALTER TABLE " + table + " ADD CONSTRAINT " + constraintName +
                            " CHECK (" + column + " IN (" + inClause + "))");
            log.info("[EnumCheckFix] {}.{} CHECK 제약 갱신 완료 ({}개 값)", table, column, values.length);
        } catch (Exception e) {
            log.warn("[EnumCheckFix] {}.{} CHECK 제약 갱신 실패 (무시): {}", table, column, e.getMessage());
        }
    }
}
