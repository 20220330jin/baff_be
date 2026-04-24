-- Align Hibernate-generated enum CHECK constraints with the current Java enums.
-- ddl-auto=update creates these constraints but does not reliably update them
-- when enum constants are added.

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
          AND constraint_table.relname IN ('reward_configs', 'reward_histories', 'user_reward_dailies')
          AND a.attname = 'reward_type'
    LOOP
        EXECUTE format(
            'ALTER TABLE %I DROP CONSTRAINT IF EXISTS %I',
            constraint_to_drop.table_name,
            constraint_to_drop.constraint_name
        );
    END LOOP;

    FOR constraint_to_drop IN
        SELECT constraint_table.relname AS table_name, c.conname AS constraint_name
        FROM pg_constraint c
        JOIN pg_class constraint_table ON constraint_table.oid = c.conrelid
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY (c.conkey)
        WHERE c.contype = 'c'
          AND constraint_table.relname = 'piece_transactions'
          AND a.attname = 'type'
    LOOP
        EXECUTE format(
            'ALTER TABLE %I DROP CONSTRAINT IF EXISTS %I',
            constraint_to_drop.table_name,
            constraint_to_drop.constraint_name
        );
    END LOOP;
END $$;

ALTER TABLE reward_configs
    ADD CONSTRAINT reward_configs_reward_type_check
    CHECK (reward_type IN (
        'WEIGHT_LOG',
        'REVIEW',
        'ATTENDANCE',
        'ATTENDANCE_STREAK',
        'ATTENDANCE_AD_BONUS',
        'WEIGHT_AD_BONUS',
        'SIGNUP_BONUS',
        'PROFILE_BONUS',
        'PROFILE_BONUS_GENDER',
        'PROFILE_BONUS_BIRTHDATE',
        'STREAK_WEIGHT',
        'GOAL_ACHIEVED',
        'BATTLE_COMPLETE',
        'MISSION_ATTENDANCE_WEEKLY',
        'MISSION_WEIGHT_WEEKLY',
        'REVIEW_AD_BONUS',
        'WEEKLY_MILESTONE_3',
        'WEEKLY_MILESTONE_5',
        'WEEKLY_MILESTONE_7',
        'EXCHANGE'
    ));

ALTER TABLE reward_histories
    ADD CONSTRAINT reward_histories_reward_type_check
    CHECK (reward_type IN (
        'WEIGHT_LOG',
        'REVIEW',
        'ATTENDANCE',
        'ATTENDANCE_STREAK',
        'ATTENDANCE_AD_BONUS',
        'WEIGHT_AD_BONUS',
        'SIGNUP_BONUS',
        'PROFILE_BONUS',
        'PROFILE_BONUS_GENDER',
        'PROFILE_BONUS_BIRTHDATE',
        'STREAK_WEIGHT',
        'GOAL_ACHIEVED',
        'BATTLE_COMPLETE',
        'MISSION_ATTENDANCE_WEEKLY',
        'MISSION_WEIGHT_WEEKLY',
        'REVIEW_AD_BONUS',
        'WEEKLY_MILESTONE_3',
        'WEEKLY_MILESTONE_5',
        'WEEKLY_MILESTONE_7',
        'EXCHANGE'
    ));

ALTER TABLE user_reward_dailies
    ADD CONSTRAINT user_reward_dailies_reward_type_check
    CHECK (reward_type IN (
        'WEIGHT_LOG',
        'REVIEW',
        'ATTENDANCE',
        'ATTENDANCE_STREAK',
        'ATTENDANCE_AD_BONUS',
        'WEIGHT_AD_BONUS',
        'SIGNUP_BONUS',
        'PROFILE_BONUS',
        'PROFILE_BONUS_GENDER',
        'PROFILE_BONUS_BIRTHDATE',
        'STREAK_WEIGHT',
        'GOAL_ACHIEVED',
        'BATTLE_COMPLETE',
        'MISSION_ATTENDANCE_WEEKLY',
        'MISSION_WEIGHT_WEEKLY',
        'REVIEW_AD_BONUS',
        'WEEKLY_MILESTONE_3',
        'WEEKLY_MILESTONE_5',
        'WEEKLY_MILESTONE_7',
        'EXCHANGE'
    ));

ALTER TABLE piece_transactions
    ADD CONSTRAINT piece_transactions_type_check
    CHECK (type IN (
        'DEPOSIT',
        'BET_DEDUCT',
        'BET_WIN',
        'BET_REFUND',
        'REWARD_WEIGHT_LOG',
        'REWARD_REVIEW',
        'REWARD_ATTENDANCE',
        'REWARD_STREAK_ATTENDANCE',
        'REWARD_AD_BONUS',
        'REWARD_MISSION',
        'REWARD_SIGNUP_BONUS',
        'REWARD_PROFILE_BONUS',
        'REWARD_PROFILE_BONUS_GENDER',
        'REWARD_PROFILE_BONUS_BIRTHDATE',
        'REWARD_WEEKLY_MILESTONE_3',
        'REWARD_WEEKLY_MILESTONE_5',
        'REWARD_WEEKLY_MILESTONE_7',
        'EXCHANGE_REQUEST',
        'EXCHANGE_SUCCESS',
        'EXCHANGE_FAILED_REFUND'
    ));
