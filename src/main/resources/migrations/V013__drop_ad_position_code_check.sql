-- ddl-auto가 enum 시절(@Enumerated AdWatchLocation) 만들어둔 CHECK 제약 제거.
-- 이후 도메인을 String varchar로 변경했지만 CHECK는 갱신되지 않음 (MEMORY: project_postgres_enum_check_pitfall).
-- 'OTHER' 같은 enum 외 값 INSERT 시 23514 위반 발생 → 본 마이그레이션으로 정리.

DO $$
DECLARE
    constraint_to_drop record;
BEGIN
    FOR constraint_to_drop IN
        SELECT constraint_table.relname AS table_name, c.conname AS constraint_name
        FROM pg_constraint c
        JOIN pg_class constraint_table ON constraint_table.oid = c.conrelid
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY (c.conkey)
        WHERE c.contype = 'c'
          AND constraint_table.relname IN ('ad_metric_banner_entry', 'ad_metric_image_entry')
          AND a.attname = 'ad_position_code'
    LOOP
        EXECUTE format(
            'ALTER TABLE %I DROP CONSTRAINT IF EXISTS %I',
            constraint_to_drop.table_name,
            constraint_to_drop.constraint_name
        );
    END LOOP;
END $$;
