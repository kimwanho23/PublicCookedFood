-- Community board features: scrap and report tables.
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

SET @board_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'board'
    LIMIT 1
);

SET @board_scrap_table_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'board_scrap'
);

SET @create_board_scrap_table_sql := IF(
    @account_table_name IS NOT NULL
    AND @board_table_name IS NOT NULL
    AND @board_scrap_table_exists = 0,
    CONCAT(
        'CREATE TABLE board_scrap (',
        'id BIGINT NOT NULL AUTO_INCREMENT, ',
        'user_id BIGINT NOT NULL, ',
        'board_id BIGINT NOT NULL, ',
        'regTime DATETIME(6) NULL, ',
        'updateTime DATETIME(6) NULL, ',
        'PRIMARY KEY (id), ',
        'CONSTRAINT uk_board_scrap_user_board UNIQUE (user_id, board_id), ',
        'INDEX idx_board_scrap_user_regtime (user_id, regTime), ',
        'INDEX idx_board_scrap_board_regtime (board_id, regTime), ',
        'CONSTRAINT fk_board_scrap_user FOREIGN KEY (user_id) REFERENCES `', @account_table_name, '` (id) ON DELETE CASCADE, ',
        'CONSTRAINT fk_board_scrap_board FOREIGN KEY (board_id) REFERENCES `', @board_table_name, '` (id) ON DELETE CASCADE',
        ')'
    ),
    'SELECT 1'
);

PREPARE stmt_create_board_scrap_table FROM @create_board_scrap_table_sql;
EXECUTE stmt_create_board_scrap_table;
DEALLOCATE PREPARE stmt_create_board_scrap_table;

SET @board_report_table_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'board_report'
);

SET @create_board_report_table_sql := IF(
    @account_table_name IS NOT NULL
    AND @board_table_name IS NOT NULL
    AND @board_report_table_exists = 0,
    CONCAT(
        'CREATE TABLE board_report (',
        'id BIGINT NOT NULL AUTO_INCREMENT, ',
        'board_id BIGINT NOT NULL, ',
        'reporter_id BIGINT NOT NULL, ',
        'reason VARCHAR(30) NOT NULL, ',
        'details VARCHAR(500) NULL, ',
        'status VARCHAR(20) NOT NULL DEFAULT ''OPEN'', ',
        'regTime DATETIME(6) NULL, ',
        'updateTime DATETIME(6) NULL, ',
        'PRIMARY KEY (id), ',
        'CONSTRAINT uk_board_report_board_reporter UNIQUE (board_id, reporter_id), ',
        'INDEX idx_board_report_status_regtime (status, regTime), ',
        'INDEX idx_board_report_reporter_regtime (reporter_id, regTime), ',
        'INDEX idx_board_report_board_regtime (board_id, regTime), ',
        'CONSTRAINT fk_board_report_board FOREIGN KEY (board_id) REFERENCES `', @board_table_name, '` (id) ON DELETE CASCADE, ',
        'CONSTRAINT fk_board_report_reporter FOREIGN KEY (reporter_id) REFERENCES `', @account_table_name, '` (id) ON DELETE CASCADE',
        ')'
    ),
    'SELECT 1'
);

PREPARE stmt_create_board_report_table FROM @create_board_report_table_sql;
EXECUTE stmt_create_board_report_table;
DEALLOCATE PREPARE stmt_create_board_report_table;
