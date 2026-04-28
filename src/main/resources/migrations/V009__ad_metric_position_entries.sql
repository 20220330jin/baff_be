-- 광고 위치별 분해 (B 배너 + I 이미지배너) — 나만그래 패턴 정합.
-- spec v0.3 §4-1 B (P1) → P0로 끌어올림. AdPositionConfig 활성 위치별 1행/일.

CREATE TABLE IF NOT EXISTS ad_metric_banner_entry (
    id BIGSERIAL PRIMARY KEY,
    metric_date DATE NOT NULL,
    ad_position_code VARCHAR(64) NOT NULL,
    impression INTEGER,
    ctr_reported NUMERIC(5,2),
    ecpm_reported INTEGER,
    revenue INTEGER,
    actor_admin_id BIGINT,
    "regDateTime" TIMESTAMP,
    "modDateTime" TIMESTAMP,
    "delYn" CHAR(1) DEFAULT 'N',
    CONSTRAINT uk_ad_metric_banner_entry UNIQUE (metric_date, ad_position_code)
);

CREATE INDEX IF NOT EXISTS idx_ad_metric_banner_entry_date
    ON ad_metric_banner_entry (metric_date);

CREATE TABLE IF NOT EXISTS ad_metric_image_entry (
    id BIGSERIAL PRIMARY KEY,
    metric_date DATE NOT NULL,
    ad_position_code VARCHAR(64) NOT NULL,
    impression INTEGER,
    ctr_reported NUMERIC(5,2),
    ecpm_reported INTEGER,
    revenue INTEGER,
    actor_admin_id BIGINT,
    "regDateTime" TIMESTAMP,
    "modDateTime" TIMESTAMP,
    "delYn" CHAR(1) DEFAULT 'N',
    CONSTRAINT uk_ad_metric_image_entry UNIQUE (metric_date, ad_position_code)
);

CREATE INDEX IF NOT EXISTS idx_ad_metric_image_entry_date
    ON ad_metric_image_entry (metric_date);
