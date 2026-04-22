-- S4-25 토스 스마트발송: templateCode + targetStrategy 필드 추가 (spec §3.3, §3.2)
-- CP1 Round 2 반영: 기존 row의 targetStrategy는 기존 로직 그대로 보존

-- 1. 컬럼 추가
ALTER TABLE smart_push_configs
    ADD COLUMN IF NOT EXISTS template_code VARCHAR(100);

ALTER TABLE smart_push_configs
    ADD COLUMN IF NOT EXISTS target_strategy VARCHAR(50)
        NOT NULL DEFAULT 'ALL_TOSS_USERS';

-- 2. 기존 row 전략 보존 (CP1 Round 2 권고)
UPDATE smart_push_configs
   SET target_strategy = 'ACTIVE_LAST_7_DAYS_NOT_ATTENDED'
 WHERE push_type = 'ATTENDANCE_REMINDER'
   AND target_strategy = 'ALL_TOSS_USERS';

UPDATE smart_push_configs
   SET target_strategy = 'BALANCE_OVER_100G'
 WHERE push_type = 'EXCHANGE_REMINDER'
   AND target_strategy = 'ALL_TOSS_USERS';
