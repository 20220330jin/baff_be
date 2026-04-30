-- R(리워드) / F(전면) 광고 위치별 분해 — 데이터 입력 폼 재구성에 따른 신규.
-- B/I와 동일 구조. (metric_date, ad_position_code) UNIQUE.

CREATE TABLE IF NOT EXISTS ad_metric_reward_entry (
    id BIGSERIAL PRIMARY KEY,
    metric_date DATE NOT NULL,
    ad_position_code VARCHAR(64) NOT NULL,
    impression INTEGER,
    ctr_reported NUMERIC(5,2),
    ecpm_reported INTEGER,
    revenue INTEGER,
    ad_id_snapshot VARCHAR(100),
    actor_admin_id BIGINT,
    "regDateTime" TIMESTAMP,
    "modDateTime" TIMESTAMP,
    "delYn" CHAR(1) DEFAULT 'N',
    CONSTRAINT uk_ad_metric_reward_entry UNIQUE (metric_date, ad_position_code)
);

CREATE INDEX IF NOT EXISTS idx_ad_metric_reward_entry_date
    ON ad_metric_reward_entry (metric_date);

CREATE TABLE IF NOT EXISTS ad_metric_interstitial_entry (
    id BIGSERIAL PRIMARY KEY,
    metric_date DATE NOT NULL,
    ad_position_code VARCHAR(64) NOT NULL,
    impression INTEGER,
    ctr_reported NUMERIC(5,2),
    ecpm_reported INTEGER,
    revenue INTEGER,
    ad_id_snapshot VARCHAR(100),
    actor_admin_id BIGINT,
    "regDateTime" TIMESTAMP,
    "modDateTime" TIMESTAMP,
    "delYn" CHAR(1) DEFAULT 'N',
    CONSTRAINT uk_ad_metric_interstitial_entry UNIQUE (metric_date, ad_position_code)
);

CREATE INDEX IF NOT EXISTS idx_ad_metric_interstitial_entry_date
    ON ad_metric_interstitial_entry (metric_date);
