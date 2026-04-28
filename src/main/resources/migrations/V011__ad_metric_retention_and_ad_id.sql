-- D1 리텐션 + 광고ID 스냅샷 추가 (나만그래 retentionD1New/Total + ad_id_snapshot 정합)
-- 위치별 row에 ad_id_snapshot 보존: 토스 콘솔 광고ID별 집계와 매칭하기 위함.

ALTER TABLE ad_metric_daily_entry
    ADD COLUMN IF NOT EXISTS retention_d1_new NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS retention_d1_total NUMERIC(5,2);

ALTER TABLE ad_metric_banner_entry
    ADD COLUMN IF NOT EXISTS ad_id_snapshot VARCHAR(100);

ALTER TABLE ad_metric_image_entry
    ADD COLUMN IF NOT EXISTS ad_id_snapshot VARCHAR(100);
