-- M-043 간헐적 단식 종료 상태 (spec management/specs/2026-11-17-fasting-end-recovery-design.md §4)
-- 순서: BE 배포(ddl-auto 가 end_status 칸을 먼저 만들 수 있다) → 이 스크립트. 재실행해도 결과가 같다.
-- 진행 중 기록(end_time IS NULL)은 건드리지 않는다 — 48시간 판정은 조회 때 서버가 계산한다.

ALTER TABLE fasting_record ADD COLUMN IF NOT EXISTS end_status VARCHAR(20);

-- 기존 종료 기록 중 48시간(2,880분) 이하 = 정상 종료
UPDATE fasting_record SET end_status = 'CONFIRMED'
 WHERE end_time IS NOT NULL AND end_status IS NULL AND actual_minutes <= 2880;

-- 48시간 초과 = 종료 누락으로 보이는 기록 · 원래 값 보존 · 통계 제외 (운영 11-17 기준 11건)
UPDATE fasting_record SET end_status = 'NEEDS_CHECK'
 WHERE end_time IS NOT NULL AND end_status IS NULL AND actual_minutes > 2880;
