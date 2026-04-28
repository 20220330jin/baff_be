-- P0 광고전략 — 일자별 토스 콘솔 reported 1행/일
-- spec v0.3 §4-3 정본. metric_date UNIQUE, BaseEntity 컬럼(regDateTime/modDateTime/delYn) 직접 명시.
-- ddl-auto 맹점 회피: NOT NULL 컬럼은 reported/metric 항목 외 메타에만 적용. 부분 입력 허용을 위해 reported는 nullable.

CREATE TABLE IF NOT EXISTS ad_metric_daily_entry (
    id BIGSERIAL PRIMARY KEY,
    metric_date DATE NOT NULL,

    -- 토스 콘솔 reported 수익 (R/F/B합산/I)
    toss_revenue_r INTEGER,
    toss_revenue_f INTEGER,
    toss_revenue_b_total INTEGER,
    toss_revenue_i INTEGER,

    -- 토스 콘솔 reported eCPM (R/F/B합산/I)
    ecpm_r_reported INTEGER,
    ecpm_f_reported INTEGER,
    ecpm_b_total_reported INTEGER,
    ecpm_i_reported INTEGER,

    -- 토스 콘솔 reported 노출
    impression_r_reported INTEGER,  -- 검증용 (truth는 DB AdWatchEvent observed)
    impression_f_reported INTEGER,  -- 검증용
    impression_b_total INTEGER,     -- truth (DB 미수집)
    impression_i INTEGER,           -- truth (DB 미수집)

    -- 토스 외 자동 미수집 항목
    new_inflow_toss INTEGER,
    avg_session_sec NUMERIC(8,2),
    benefits_tab_inflow INTEGER,

    -- 메타
    actor_admin_id BIGINT,
    "regDateTime" TIMESTAMP,
    "modDateTime" TIMESTAMP,
    "delYn" CHAR(1) DEFAULT 'N',

    CONSTRAINT uk_ad_metric_daily_entry_metric_date UNIQUE (metric_date)
);

CREATE INDEX IF NOT EXISTS idx_ad_metric_daily_entry_metric_date
    ON ad_metric_daily_entry (metric_date);
