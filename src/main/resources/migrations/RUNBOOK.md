# S3 Account Integration — Migration Runbook

> 대상 DB: Render PostgreSQL (prod)
>
> **배포 순서 원칙 (2026-04-21 hotfix 이후)**:
> V001을 **앱 배포 전**에 실행하는 것이 원칙 (권장). Hibernate ddl-auto=update가 NOT NULL 컬럼을 기존 row에
> 추가하려다 실패하는 맹점 때문. V001/V002 모두 IF NOT EXISTS 기반이라 재실행 안전하며, self-contained로
> 컬럼 생성/backfill/제약 추가를 모두 처리.

## 1. 배포 순서 (권장: SQL 선실행)

### 1.1 V001 SQL 선실행

```bash
psql $DATABASE_URL -f src/main/resources/migrations/V001__account_integration_schema.sql
```

V001이 처리하는 것 (self-contained, 2026-04-21 hotfix):
- **§0 user_attendances.source 자체 마이그레이션** — NULLABLE + DEFAULT → backfill → NOT NULL → CHECK 제약 순차 적용 (DO 블록으로 멱등)
- §1 `account_links` partial unique 2종 (테이블 존재 시)
- §2 `account_merge_logs` retention 인덱스 (테이블 존재 시)
- §3 `link_tokens` idempotency 인덱스 (테이블 존재 시)
- §4 토스 유저 출석 `source='TOSS'` backfill

**주의**: `account_links`/`account_merge_logs`/`link_tokens` 테이블 자체는 Hibernate가 생성함. 따라서 최초 배포 시 테이블이 아직 없으면 §1~3 CREATE INDEX가 실패할 수 있음 → 1.4 대안 사용.

### 1.2 V002 SQL 실행

```bash
psql $DATABASE_URL -f src/main/resources/migrations/V002__link_token_toss_user_key_and_nonce.sql
```

- link_tokens에 `toss_user_key` + `prepare_nonce_hash` 컬럼 추가
- `ix_link_tokens_toss_user_key` partial index 생성
- V001 이후 실행 (link_tokens 테이블 존재 필요)

### 1.3 앱 배포

- baff_be main 브랜치 push → Render 자동 배포
- Hibernate ddl-auto가 스키마 차이 대부분 skip (이미 반영됨)
- 부팅 로그에 `source ... contains null values` 등 DDL WARN 없어야 함

### 1.4 대안: 앱 선배포 (최초 배포 혹은 테이블 미존재 시)

1. 앱 배포 — Hibernate가 신규 테이블 생성 시도
2. `user_attendances.source` NOT NULL 추가 실패 로그 발생 가능 (2026-04-21 실제 발생)
3. V001 즉시 실행 — §0이 컬럼/제약 복구
4. 앱 재시작 — Hibernate가 스키마 OK 판단
5. V002 실행

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

### 3.1 Phase 1.5 배포 직후 (기본값 `baff.account-link.enabled=false`) — CP2 Round 3 P1-2 반영

**기대값**: 4개 endpoint 모두 **404 응답** + service 미호출. feature flag가 기본 disabled이므로 정상 동작.

```bash
curl -i -X POST $HOST/api/account/link/issue-token
# 기대: HTTP/1.1 404 Not Found

curl -i -X POST $HOST/api/account/link/prepare \
     -H 'Content-Type: application/json' \
     -d '{"linkToken":"x","authorizationCode":"x","referrer":"x"}'
# 기대: HTTP/1.1 404 Not Found

curl -i -X POST $HOST/api/account/link/confirm \
     -H 'Content-Type: application/json' \
     -d '{"linkToken":"x","idempotencyKey":"x","nonce":"x"}'
# 기대: HTTP/1.1 404 Not Found

curl -i -X PATCH $HOST/api/account/link/dismiss-banner
# 기대: HTTP/1.1 404 Not Found
```

- 404 이외 응답이면 feature flag 로딩 실패 → 즉시 롤백 검토
- 로그인 경로는 영향 없음 (Resolver 미변경) — `/api/toss/login` 수동 curl 회귀 확인

### 3.2 Enable 스모크 테스트 (별도 절차 — 릴리즈 승인 이후만 수행)

**선행 조건** (Plan v3 Task 1.5-9 활성화 조건 5단계 모두 충족):
1. Phase 1.5 BE merge + CP2 Round 3 Sign-off ✅
2. Phase 2 FE 구현 완료
3. CP2-FE Sign-off
4. 내부 테스트 통과
5. 대표님 릴리즈 승인

**활성화 절차**:
- `application-prod.yml`에 `baff.account-link.enabled: true` 설정 후 재배포 (또는 Render 환경변수)
- 활성화 후 스모크:
  - `/api/account/link/issue-token` 호출 (로그인 세션 필수) → 200 + linkToken 반환
  - `/api/account/link/prepare` 호출 (유효 authorizationCode + 활성 배틀 없는 테스트 계정) → canLink=true + diff + nonce
  - `/api/account/link/confirm` 호출 (prepare 응답 nonce 그대로) → success=true + primaryUserId
- 어느 하나라도 실패하면 `enabled: false`로 즉시 롤백

## 4. 모니터링 (배포 후 24시간)
- Render 로그에서 로그인 실패 + Account link API 에러율 watch
- 이상 시 즉시 롤백

## 5. 롤백 (치명 버그 시)

```sql
-- 5.0 V002 롤백 (V001보다 먼저)
DROP INDEX IF EXISTS ix_link_tokens_toss_user_key;
ALTER TABLE link_tokens DROP COLUMN IF EXISTS toss_user_key;
ALTER TABLE link_tokens DROP COLUMN IF EXISTS prepare_nonce_hash;

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
