-- V002: link_tokens에 toss_user_key + prepare_nonce_hash 컬럼 추가 (Phase 1.5 v3)
--
-- 배경:
--   Phase 2 FE Plan Review Round 1 P0 — FE는 appLogin()이 반환하는 {authorizationCode, referrer}만
--   다룰 수 있고 raw tossUserKey를 생성할 수 없음. prepare가 authorizationCode를 mTLS로 교환하여
--   도출한 tossUserKey를 link_tokens에 영구 저장해 confirm에서 재사용한다.
--
--   Phase 2 FE Plan Review Round 2 P0 — confirm이 linkToken만으로 irreversible merge를 실행하면
--   Secondary 소유/동의 증명이 소실됨. prepare가 nonce를 발급하고 sha256 해시를 link_tokens에 저장,
--   confirm은 FE가 보낸 nonce를 해시 비교로 검증한다.
--
-- 실행 순서: V001 이후 반드시 V001__account_integration_schema.sql이 선행되어야 함.
--
-- 참고: spec.md §3.1 link_tokens 테이블, §4.2 prepare, §4.3 confirm 트랜잭션 2.1단계

ALTER TABLE link_tokens ADD COLUMN toss_user_key VARCHAR(255) NULL;
ALTER TABLE link_tokens ADD COLUMN prepare_nonce_hash VARCHAR(64) NULL;

CREATE INDEX ix_link_tokens_toss_user_key
  ON link_tokens(toss_user_key)
  WHERE toss_user_key IS NOT NULL;
