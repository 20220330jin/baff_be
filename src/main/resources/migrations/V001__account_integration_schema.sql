-- V001: S3 Account Integration Schema Migration
-- Target: PostgreSQL (production)
-- 실행 순서:
--   1. 신 엔티티 반영된 앱 배포 (ddl-auto=update 가정). 자동 생성:
--      - account_links, account_merge_logs, link_tokens 테이블
--      - users.primary_user_id, users.account_link_banner_dismissed_at 컬럼
--      - user_attendances.source 컬럼 (DEFAULT 'WEB')
--      - user_flag ux_user_flag_user_flagkey unique
--      - UserStatus/InviteStatus/AttendanceSource enum 값 (STRING 저장이므로 DB 스키마 영향 없음)
--   2. 본 SQL 실행 (partial unique + attendance source migration)
--   3. 검증 쿼리 수동 실행

-- =======================================================================
-- 1. account_links partial unique (JPA @UniqueConstraint로는 표현 불가)
-- =======================================================================

CREATE UNIQUE INDEX IF NOT EXISTS ux_account_links_active_provider_user
    ON account_links(provider, provider_user_id) WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX IF NOT EXISTS ux_account_links_active_user
    ON account_links(user_id) WHERE status = 'ACTIVE';

-- =======================================================================
-- 2. account_merge_logs retention 배치 인덱스
-- =======================================================================

CREATE INDEX IF NOT EXISTS ix_account_merge_logs_retention
    ON account_merge_logs(retention_until) WHERE archived_at IS NULL;

-- =======================================================================
-- 3. link_tokens idempotency 조회 최적화
-- =======================================================================

CREATE INDEX IF NOT EXISTS ix_link_tokens_idempotency
    ON link_tokens(idempotency_key);

-- =======================================================================
-- 4. user_attendances source 마이그레이션
--    기존 토스 유저의 출석 기록을 'TOSS'로 분류
--    (spec §3.2: socialId LIKE 'toss_%' 미사용, provider/platform 기준)
-- =======================================================================

UPDATE user_attendances ua
SET source = 'TOSS'
FROM users u
WHERE ua.user_id = u."userId"
  AND (u.provider = 'toss' OR u.platform = 'TOSS')
  AND ua.source = 'WEB';

-- =======================================================================
-- 5. 검증 쿼리 (수동 실행 — RUNBOOK 참조)
-- =======================================================================

-- SELECT
--     (SELECT COUNT(*) FROM user_attendances ua
--        JOIN users u ON ua.user_id = u."userId"
--       WHERE u.provider = 'toss' OR u.platform = 'TOSS') AS expected,
--     (SELECT COUNT(*) FROM user_attendances WHERE source = 'TOSS') AS actual;
-- expected == actual 필수
