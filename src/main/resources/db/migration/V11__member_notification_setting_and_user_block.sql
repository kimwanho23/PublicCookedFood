-- Member notification preference and user block feature.
-- This script is idempotent and safe to run repeatedly.

SET @member_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'member'
    LIMIT 1
);

SET @legacy_user_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'user'
    LIMIT 1
);

SET @account_table_name := IF(
    @member_table_name IS NOT NULL,
    @member_table_name,
    @legacy_user_table_name
);

SET @has_notification_enabled := (
    SELECT IF(
        @account_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @account_table_name
              AND column_name = 'notificationEnabled'
        )
    )
);

SET @add_notification_enabled_sql := IF(
    @account_table_name IS NOT NULL
    AND @has_notification_enabled = 0,
    CONCAT(
        'ALTER TABLE `', @account_table_name, '` ',
        'ADD COLUMN notificationEnabled BIT(1) NOT NULL DEFAULT b''1'''
    ),
    'SELECT 1'
);

PREPARE stmt_add_notification_enabled FROM @add_notification_enabled_sql;
EXECUTE stmt_add_notification_enabled;
DEALLOCATE PREPARE stmt_add_notification_enabled;

SET @ensure_notification_enabled_default_sql := IF(
    @account_table_name IS NOT NULL,
    CONCAT(
        'UPDATE `', @account_table_name, '` ',
        'SET notificationEnabled = b''1'' ',
        'WHERE notificationEnabled IS NULL'
    ),
    'SELECT 1'
);

PREPARE stmt_ensure_notification_enabled_default FROM @ensure_notification_enabled_default_sql;
EXECUTE stmt_ensure_notification_enabled_default;
DEALLOCATE PREPARE stmt_ensure_notification_enabled_default;

SET @user_block_table_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'user_block'
);

SET @create_user_block_table_sql := IF(
    @account_table_name IS NOT NULL
    AND @user_block_table_exists = 0,
    CONCAT(
        'CREATE TABLE user_block (',
        'id BIGINT NOT NULL AUTO_INCREMENT, ',
        'blocker_id BIGINT NOT NULL, ',
        'blocked_id BIGINT NOT NULL, ',
        'regTime DATETIME(6) NULL, ',
        'updateTime DATETIME(6) NULL, ',
        'PRIMARY KEY (id), ',
        'CONSTRAINT uk_user_block_blocker_blocked UNIQUE (blocker_id, blocked_id), ',
        'INDEX idx_user_block_blocker_regtime (blocker_id, regTime), ',
        'INDEX idx_user_block_blocked_regtime (blocked_id, regTime), ',
        'CONSTRAINT fk_user_block_blocker FOREIGN KEY (blocker_id) REFERENCES `', @account_table_name, '` (id) ON DELETE CASCADE, ',
        'CONSTRAINT fk_user_block_blocked FOREIGN KEY (blocked_id) REFERENCES `', @account_table_name, '` (id) ON DELETE CASCADE',
        ')'
    ),
    'SELECT 1'
);

PREPARE stmt_create_user_block_table FROM @create_user_block_table_sql;
EXECUTE stmt_create_user_block_table;
DEALLOCATE PREPARE stmt_create_user_block_table;
