-- 토스 콘솔 reported — 신규/전체 유저 (나만그래 newUsers/totalUsers 정합)
-- 토스 콘솔에서 운영자가 직접 보고 옮겨 적는 항목. DB 자동 집계와 별개.

ALTER TABLE ad_metric_daily_entry
    ADD COLUMN IF NOT EXISTS new_users_reported INTEGER,
    ADD COLUMN IF NOT EXISTS total_users_reported INTEGER;
