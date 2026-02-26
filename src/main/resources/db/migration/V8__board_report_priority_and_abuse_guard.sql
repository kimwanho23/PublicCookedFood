-- Board report queue prioritization and abuse-guard metadata.
-- This script is idempotent and safe to run repeatedly.

SET @board_report_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'board_report'
    LIMIT 1
);

SET @has_priority_score := (
    SELECT IF(
        @board_report_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @board_report_table_name
              AND column_name = 'priority_score'
        )
    )
);

SET @has_is_suspicious := (
    SELECT IF(
        @board_report_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @board_report_table_name
              AND column_name = 'is_suspicious'
        )
    )
);

SET @add_priority_score_sql := IF(
    @board_report_table_name IS NOT NULL
    AND @has_priority_score = 0,
    CONCAT(
        'ALTER TABLE `', @board_report_table_name, '` ',
        'ADD COLUMN priority_score INT NOT NULL DEFAULT 0 AFTER processed_time'
    ),
    'SELECT 1'
);

PREPARE stmt_add_priority_score FROM @add_priority_score_sql;
EXECUTE stmt_add_priority_score;
DEALLOCATE PREPARE stmt_add_priority_score;

SET @add_is_suspicious_sql := IF(
    @board_report_table_name IS NOT NULL
    AND @has_is_suspicious = 0,
    CONCAT(
        'ALTER TABLE `', @board_report_table_name, '` ',
        'ADD COLUMN is_suspicious BIT(1) NOT NULL DEFAULT b''0'' AFTER priority_score'
    ),
    'SELECT 1'
);

PREPARE stmt_add_is_suspicious FROM @add_is_suspicious_sql;
EXECUTE stmt_add_is_suspicious;
DEALLOCATE PREPARE stmt_add_is_suspicious;

SET @has_priority_score := (
    SELECT IF(
        @board_report_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @board_report_table_name
              AND column_name = 'priority_score'
        )
    )
);

SET @has_is_suspicious := (
    SELECT IF(
        @board_report_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @board_report_table_name
              AND column_name = 'is_suspicious'
        )
    )
);

SET @priority_index_exists := (
    SELECT IF(
        @board_report_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = @board_report_table_name
              AND index_name = 'idx_board_report_status_priority_regtime'
        )
    )
);

SET @add_priority_index_sql := IF(
    @board_report_table_name IS NOT NULL
    AND @has_priority_score > 0
    AND @has_is_suspicious > 0
    AND @priority_index_exists = 0,
    CONCAT(
        'ALTER TABLE `', @board_report_table_name, '` ',
        'ADD INDEX idx_board_report_status_priority_regtime (status, is_suspicious, priority_score, regTime)'
    ),
    'SELECT 1'
);

PREPARE stmt_add_priority_index FROM @add_priority_index_sql;
EXECUTE stmt_add_priority_index;
DEALLOCATE PREPARE stmt_add_priority_index;
