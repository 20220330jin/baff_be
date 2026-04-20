# S3 Account Integration — Migration Runbook

> 대상 DB: Render PostgreSQL (prod)
> 선행: Phase 1 BE 배포 (ddl-auto=update로 신 엔티티/컬럼 자동 반영)

## 1. 배포 순서

### 1.1 앱 배포
- baff_be `feat/s3-account-integration-phase1` 브랜치 merge 후 main 배포
- Render 자동 배포 모니터링: Spring Boot 재시작 + Hibernate ddl-auto 실행
- 자동 생성 확인:
  - 테이블: `account_links`, `account_merge_logs`, `link_tokens`
  - `users` 컬럼: `primary_user_id`, `account_link_banner_dismissed_at`
  - `user_attendances.source` (DEFAULT 'WEB')
  - `user_flag` 테이블의 `ux_user_flag_user_flagkey` unique

### 1.2 V001 SQL 실행
```bash
psql $DATABASE_URL -f src/main/resources/migrations/V001__account_integration_schema.sql
```

### 1.3 검증 쿼리 (필수)
```sql
SELECT
    (SELECT COUNT(*) FROM user_attendances ua
       JOIN users u ON ua.user_id = u."userId"
      WHERE u.provider = 'toss' OR u.platform = 'TOSS') AS expected,
    (SELECT COUNT(*) FROM user_attendances WHERE source = 'TOSS') AS actual;
```
- **expected == actual 일치 필수**
- 불일치 시 5번 롤백 실행

## 2. Partial Unique 검증
```sql
-- account_links partial unique 존재 확인
SELECT indexname, indexdef FROM pg_indexes
 WHERE tablename = 'account_links'
   AND indexname IN ('ux_account_links_active_provider_user', 'ux_account_links_active_user');
-- 2건 반환 기대
```

## 3. 배포 후 스모크 테스트
- `/api/account/link/issue-token` 호출 → 200 + linkToken 반환
- `/api/account/link/prepare` 호출 (활성 배틀 없는 테스트 계정) → canLink=true + diff
- 로그인 경로 정상 동작 확인 (Resolver 전환 영향)

## 4. 모니터링 (배포 후 24시간)
- Render 로그에서 로그인 실패 + Account link API 에러율 watch
- 이상 시 즉시 롤백

## 5. 롤백 (치명 버그 시)

```sql
-- 5.1 partial unique index 제거
DROP INDEX IF EXISTS ux_account_links_active_provider_user;
DROP INDEX IF EXISTS ux_account_links_active_user;
DROP INDEX IF EXISTS ix_account_merge_logs_retention;
DROP INDEX IF EXISTS ix_link_tokens_idempotency;

-- 5.2 신규 테이블 삭제
DROP TABLE IF EXISTS link_tokens CASCADE;
DROP TABLE IF EXISTS account_merge_logs CASCADE;
DROP TABLE IF EXISTS account_links CASCADE;

-- 5.3 컬럼 제거
ALTER TABLE user_attendances DROP COLUMN IF EXISTS source;
ALTER TABLE users DROP COLUMN IF EXISTS primary_user_id;
ALTER TABLE users DROP COLUMN IF EXISTS account_link_banner_dismissed_at;

-- 5.4 UserFlag unique 제거 (선택)
-- ALTER TABLE user_flag DROP CONSTRAINT IF EXISTS ux_user_flag_user_flagkey;
```

- 코드 롤백: PR revert 또는 main에서 이전 커밋으로 재배포
- UserStatus.MERGED enum 값은 ENUMTYPE.STRING 저장이라 DB에 없으므로 롤백 불필요 (사용하지 않으면 무해)

## 6. 30일 수동 복구 (개별 유저 이슈 대응)

병합 후 30일 이내 운영자 수동 복구가 필요한 경우:

```sql
-- 6.1 병합 정보 조회
SELECT * FROM account_merge_logs
 WHERE primary_user_id = :primary_id OR secondary_user_id = :secondary_id;

-- 6.2 AccountLink revoke
UPDATE account_links
   SET status = 'REVOKED', revoked_at = NOW()
 WHERE user_id = :primary_id AND provider = 'toss';

-- 6.3 Secondary UserB 복구
UPDATE users
   SET status = 'ACTIVE', primary_user_id = NULL
 WHERE "userId" = :secondary_id;

-- 6.4 주의: 병합으로 이관된 FK는 수동 복구 불가 (spec §6.4)
-- data_summary JSON 참조하여 개별 판단 필요
```

접근 로그 기록: 운영자 수동 복구 시 admin audit log 필수 (spec §3.1 접근통제).
