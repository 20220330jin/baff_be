-- S6-2 FeatureAccessConfig
-- 기능별 접근 제어 — enabled(전체 ON/OFF) + loginRequired(로그인 요구)

CREATE TABLE IF NOT EXISTS feature_access_config (
    id BIGSERIAL PRIMARY KEY,
    feature_key VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    login_required BOOLEAN NOT NULL DEFAULT FALSE,
    description VARCHAR(100),
    "regDateTime" TIMESTAMP,
    "modDateTime" TIMESTAMP,
    "delYn" CHAR(1) DEFAULT 'N',
    CONSTRAINT uk_feature_access_config_feature_key UNIQUE (feature_key)
);

CREATE INDEX IF NOT EXISTS idx_feature_access_config_feature_key ON feature_access_config (feature_key);
