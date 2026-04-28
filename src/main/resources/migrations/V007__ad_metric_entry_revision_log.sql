-- P0 광고전략 — D+7 이후 수정 시에만 적재되는 revision log
-- spec v0.3 §4-3. D+7 미만 수정은 일반 update only, D+7 이후만 row 적재 + reason NOT NULL.
-- diff는 changed columns only, schema_version으로 포맷 진화 추적.

CREATE TABLE IF NOT EXISTS ad_metric_entry_revision_log (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(64) NOT NULL,           -- 'AdMetricDailyEntry' | 'AdMetricBannerEntry' (P1)
    row_metric_date DATE NOT NULL,
    row_ad_position_code VARCHAR(64),          -- BannerEntry 전용 (P1)
    diff JSONB NOT NULL,                       -- {"changed": {col: {"before": x, "after": y}, ...}}
    schema_version INTEGER NOT NULL DEFAULT 1,
    reason TEXT NOT NULL,                      -- D+7 이후 수정 사유 (NOT NULL 강제)
    actor_admin_id BIGINT NOT NULL,

    "regDateTime" TIMESTAMP,
    "modDateTime" TIMESTAMP,
    "delYn" CHAR(1) DEFAULT 'N'
);

CREATE INDEX IF NOT EXISTS idx_ad_metric_revision_log_lookup
    ON ad_metric_entry_revision_log (table_name, row_metric_date);
