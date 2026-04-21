-- V001: S3 Account Integration Schema Migration (self-contained)
-- Target: PostgreSQL (production)
--
-- 2026-04-21 Hotfix 사유:
--   원본 V001은 "앱 배포 → Hibernate ddl-auto가 user_attendances.source 컬럼을 먼저 생성 → 본 SQL UPDATE"
--   순서를 전제했으나, 엔티티가 @Column(nullable=false)이므로 Hibernate가 NOT NULL 컬럼을 기존 row에
--   추가하려다 PSQLException "column contains null values"로 실패 (실제 발생: 2026-04-21 Render).
--   본 V001은 이 맹점을 제거하기 위해 source 컬럼 생성/backfill/제약 추가를 자체적으로 완결.
--   모든 DDL은 IF NOT EXISTS 기반으로 재실행 안전.
--
-- 권장 실행 순서 (다음 배포부터):
--   1. 본 V001 SQL을 앱 배포 전에 수동 실행 (스키마 선반영)
--   2. 앱 배포 — Hibernate ddl-auto가 스키마 차이를 대부분 인식하지 못함 (이미 맞음)
--   3. V002 SQL 실행 (link_tokens 확장)
--   4. 검증 쿼리 수동 실행 (아래 섹션 5 주석 해제)
--
-- 최초 배포 또는 테이블 미존재 시:
--   account_links / account_merge_logs / link_tokens 테이블 자체는 Hibernate가 생성하므로,
--   최초 배포는 "앱 배포 → V001 실행 → 앱 재시작 → V002" 순서로도 가능 (RUNBOOK §1.4 참조).

-- =======================================================================
-- 0. user_attendances.source 컬럼 self-contained 마이그레이션 (2026-04-21 hotfix)
--    Hibernate ddl-auto가 NOT NULL 추가하다 실패하는 문제 차단.
-- =======================================================================

-- 0.1 NULLABLE + DEFAULT 'WEB'로 컬럼 추가 (기존 row 자동 'WEB' 채움)
ALTER TABLE user_attendances
    ADD COLUMN IF NOT EXISTS source VARCHAR(20) DEFAULT 'WEB';

-- 0.2 DEFAULT로 안 채워진 row 방어 backfill (재실행 안전)
UPDATE user_attendances SET source = 'WEB' WHERE source IS NULL;

-- 0.3 NOT NULL 제약 (이미 NOT NULL이면 skip)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_name = 'user_attendances'
           AND column_name = 'source'
           AND is_nullable = 'YES'
    ) THEN
        EXECUTE 'ALTER TABLE user_attendances ALTER COLUMN source SET NOT NULL';
    END IF;
END$$;

-- 0.4 CHECK 제약 (이미 있으면 skip)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
         WHERE constraint_name = 'chk_user_attendances_source'
           AND table_name = 'user_attendances'
    ) THEN
        EXECUTE 'ALTER TABLE user_attendances
                 ADD CONSTRAINT chk_user_attendances_source
                 CHECK (source IN (''WEB'',''TOSS'',''MERGED_TOSS''))';
    END IF;
END$$;

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
