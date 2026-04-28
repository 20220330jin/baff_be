-- 광고 종류별 시청률(ctr) 4축 추가 — 나만그래 패턴 정합 (R/F/B합산/I 각각 노출·시청률·eCPM·수익 4축).
-- spec §3-3의 시청률은 R/F는 자동 계산(완료/노출)도 가능하나, 운영상 토스 콘솔 reported값을 그대로 입력하는 편이 낮은 마찰.
-- 0~100 범위 percent (예: 48.36).

ALTER TABLE ad_metric_daily_entry
    ADD COLUMN IF NOT EXISTS ctr_r_reported NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS ctr_f_reported NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS ctr_b_total_reported NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS ctr_i_reported NUMERIC(5,2);
