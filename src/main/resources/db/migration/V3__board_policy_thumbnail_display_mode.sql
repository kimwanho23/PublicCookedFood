-- Add board thumbnail display mode policy (LEFT/HOVER/NONE).
-- This script is idempotent and safe to run repeatedly.

SET @board_policy_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'board_policy'
    LIMIT 1
);

SET @thumbnail_display_mode_column_exists := (
    SELECT IF(
        @board_policy_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @board_policy_table_name
              AND column_name = 'thumbnail_display_mode'
        )
    )
);

SET @add_thumbnail_display_mode_column_sql := IF(
    @board_policy_table_name IS NOT NULL
    AND @thumbnail_display_mode_column_exists = 0,
    CONCAT(
        'ALTER TABLE `', @board_policy_table_name, '` ',
        'ADD COLUMN thumbnail_display_mode VARCHAR(20) NOT NULL DEFAULT ''LEFT'' '
    ),
    'SELECT 1'
);

PREPARE stmt_add_thumbnail_display_mode_column FROM @add_thumbnail_display_mode_column_sql;
EXECUTE stmt_add_thumbnail_display_mode_column;
DEALLOCATE PREPARE stmt_add_thumbnail_display_mode_column;

SET @normalize_thumbnail_display_mode_sql := IF(
    @board_policy_table_name IS NOT NULL,
    CONCAT(
        'UPDATE `', @board_policy_table_name, '` ',
        'SET thumbnail_display_mode = UPPER(thumbnail_display_mode) ',
        'WHERE thumbnail_display_mode IS NOT NULL'
    ),
    'SELECT 1'
);

PREPARE stmt_normalize_thumbnail_display_mode FROM @normalize_thumbnail_display_mode_sql;
EXECUTE stmt_normalize_thumbnail_display_mode;
DEALLOCATE PREPARE stmt_normalize_thumbnail_display_mode;

SET @sanitize_thumbnail_display_mode_sql := IF(
    @board_policy_table_name IS NOT NULL,
    CONCAT(
        'UPDATE `', @board_policy_table_name, '` ',
        'SET thumbnail_display_mode = ''LEFT'' ',
        'WHERE thumbnail_display_mode IS NULL ',
        'OR thumbnail_display_mode = '''' ',
        'OR thumbnail_display_mode NOT IN (''LEFT'', ''HOVER'', ''NONE'')'
    ),
    'SELECT 1'
);

PREPARE stmt_sanitize_thumbnail_display_mode FROM @sanitize_thumbnail_display_mode_sql;
EXECUTE stmt_sanitize_thumbnail_display_mode;
DEALLOCATE PREPARE stmt_sanitize_thumbnail_display_mode;
