-- Board report moderation enhancement:
-- add processor metadata fields for admin handling history.
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

SET @board_report_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'board_report'
    LIMIT 1
);

SET @board_report_has_processed_by := (
    SELECT IF(
        @board_report_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @board_report_table_name
              AND column_name = 'processed_by'
        )
    )
);

SET @board_report_has_processed_note := (
    SELECT IF(
        @board_report_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @board_report_table_name
              AND column_name = 'processed_note'
        )
    )
);

SET @board_report_has_processed_time := (
    SELECT IF(
        @board_report_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @board_report_table_name
              AND column_name = 'processed_time'
        )
    )
);

SET @add_board_report_processed_by_sql := IF(
    @board_report_table_name IS NOT NULL
    AND @board_report_has_processed_by = 0,
    CONCAT(
        'ALTER TABLE `', @board_report_table_name, '` ',
        'ADD COLUMN processed_by BIGINT NULL AFTER status'
    ),
    'SELECT 1'
);

PREPARE stmt_add_board_report_processed_by FROM @add_board_report_processed_by_sql;
EXECUTE stmt_add_board_report_processed_by;
DEALLOCATE PREPARE stmt_add_board_report_processed_by;

SET @add_board_report_processed_note_sql := IF(
    @board_report_table_name IS NOT NULL
    AND @board_report_has_processed_note = 0,
    CONCAT(
        'ALTER TABLE `', @board_report_table_name, '` ',
        'ADD COLUMN processed_note VARCHAR(500) NULL AFTER processed_by'
    ),
    'SELECT 1'
);

PREPARE stmt_add_board_report_processed_note FROM @add_board_report_processed_note_sql;
EXECUTE stmt_add_board_report_processed_note;
DEALLOCATE PREPARE stmt_add_board_report_processed_note;

SET @add_board_report_processed_time_sql := IF(
    @board_report_table_name IS NOT NULL
    AND @board_report_has_processed_time = 0,
    CONCAT(
        'ALTER TABLE `', @board_report_table_name, '` ',
        'ADD COLUMN processed_time DATETIME(6) NULL AFTER processed_note'
    ),
    'SELECT 1'
);

PREPARE stmt_add_board_report_processed_time FROM @add_board_report_processed_time_sql;
EXECUTE stmt_add_board_report_processed_time;
DEALLOCATE PREPARE stmt_add_board_report_processed_time;

SET @board_report_has_processed_by := (
    SELECT IF(
        @board_report_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @board_report_table_name
              AND column_name = 'processed_by'
        )
    )
);

SET @board_report_has_processed_time := (
    SELECT IF(
        @board_report_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @board_report_table_name
              AND column_name = 'processed_time'
        )
    )
);

SET @board_report_processed_index_exists := (
    SELECT IF(
        @board_report_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = @board_report_table_name
              AND index_name = 'idx_board_report_processed_by_time'
        )
    )
);

SET @add_board_report_processed_index_sql := IF(
    @board_report_table_name IS NOT NULL
    AND @board_report_has_processed_by > 0
    AND @board_report_has_processed_time > 0
    AND @board_report_processed_index_exists = 0,
    CONCAT(
        'ALTER TABLE `', @board_report_table_name, '` ',
        'ADD INDEX idx_board_report_processed_by_time (processed_by, processed_time)'
    ),
    'SELECT 1'
);

PREPARE stmt_add_board_report_processed_index FROM @add_board_report_processed_index_sql;
EXECUTE stmt_add_board_report_processed_index;
DEALLOCATE PREPARE stmt_add_board_report_processed_index;

SET @board_report_processed_fk_exists := (
    SELECT IF(
        @board_report_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.table_constraints
            WHERE constraint_schema = DATABASE()
              AND table_name = @board_report_table_name
              AND constraint_type = 'FOREIGN KEY'
              AND constraint_name = 'fk_board_report_processed_by'
        )
    )
);

SET @add_board_report_processed_fk_sql := IF(
    @board_report_table_name IS NOT NULL
    AND @account_table_name IS NOT NULL
    AND @board_report_has_processed_by > 0
    AND @board_report_processed_fk_exists = 0,
    CONCAT(
        'ALTER TABLE `', @board_report_table_name, '` ',
        'ADD CONSTRAINT fk_board_report_processed_by ',
        'FOREIGN KEY (processed_by) REFERENCES `', @account_table_name, '` (id) ON DELETE SET NULL'
    ),
    'SELECT 1'
);

PREPARE stmt_add_board_report_processed_fk FROM @add_board_report_processed_fk_sql;
EXECUTE stmt_add_board_report_processed_fk;
DEALLOCATE PREPARE stmt_add_board_report_processed_fk;
