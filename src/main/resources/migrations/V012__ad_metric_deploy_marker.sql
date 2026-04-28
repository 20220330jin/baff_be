-- 배포 마커 (나만그래 deploy_marker 정합)
-- 일자별 빌드 버전 + 메모 — daily 분석에서 §6 배포 영향 귀인용

CREATE TABLE IF NOT EXISTS ad_metric_deploy_marker (
    id BIGSERIAL PRIMARY KEY,
    metric_date DATE NOT NULL,
    deploy_version VARCHAR(50) NOT NULL,
    deploy_note TEXT,
    actor_admin_id BIGINT,
    "regDateTime" TIMESTAMP,
    "modDateTime" TIMESTAMP,
    "delYn" CHAR(1) DEFAULT 'N',
    CONSTRAINT uk_ad_metric_deploy_marker UNIQUE (metric_date, deploy_version)
);

CREATE INDEX IF NOT EXISTS idx_ad_metric_deploy_marker_date
    ON ad_metric_deploy_marker (metric_date);
