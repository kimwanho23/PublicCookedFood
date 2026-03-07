SET @board_report_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'board_report'
    LIMIT 1
);

SET @board_report_unique_index_exists := (
    SELECT IF(
        @board_report_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = @board_report_table_name
              AND index_name = 'uk_board_report_board_reporter'
              AND non_unique = 0
        )
    )
);

SET @drop_board_report_unique_index_sql := IF(
    @board_report_table_name IS NOT NULL
    AND @board_report_unique_index_exists > 0,
    CONCAT(
        'ALTER TABLE `', @board_report_table_name, '` ',
        'DROP INDEX `uk_board_report_board_reporter`'
    ),
    'SELECT 1'
);

PREPARE stmt_drop_board_report_unique_index FROM @drop_board_report_unique_index_sql;
EXECUTE stmt_drop_board_report_unique_index;
DEALLOCATE PREPARE stmt_drop_board_report_unique_index;

SET @board_report_status_index_exists := (
    SELECT IF(
        @board_report_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = @board_report_table_name
              AND index_name = 'idx_board_report_board_reporter_status_regtime'
        )
    )
);

SET @add_board_report_status_index_sql := IF(
    @board_report_table_name IS NOT NULL
    AND @board_report_status_index_exists = 0,
    CONCAT(
        'ALTER TABLE `', @board_report_table_name, '` ',
        'ADD INDEX idx_board_report_board_reporter_status_regtime (board_id, reporter_id, status, regTime)'
    ),
    'SELECT 1'
);

PREPARE stmt_add_board_report_status_index FROM @add_board_report_status_index_sql;
EXECUTE stmt_add_board_report_status_index;
DEALLOCATE PREPARE stmt_add_board_report_status_index;
