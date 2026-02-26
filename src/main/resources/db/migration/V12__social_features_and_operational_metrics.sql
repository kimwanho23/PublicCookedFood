-- Social feature expansion:
-- - board mention/report-result notification readiness
-- - bookmark folder/tag metadata
-- - board auto-hide by reports
-- - recipe review
-- - user activity log
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

SET @notification_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'notification'
    LIMIT 1
);

SET @bookmark_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'bookmark'
    LIMIT 1
);

SET @recipe_info_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'recipe_info'
    LIMIT 1
);

SET @has_board_hidden_column := (
    SELECT IF(
        @board_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @board_table_name
              AND column_name = 'is_hidden_by_report'
        )
    )
);

SET @add_board_hidden_column_sql := IF(
    @board_table_name IS NOT NULL
    AND @has_board_hidden_column = 0,
    CONCAT(
        'ALTER TABLE `', @board_table_name, '` ',
        'ADD COLUMN is_hidden_by_report BIT(1) NOT NULL DEFAULT b''0'''
    ),
    'SELECT 1'
);

PREPARE stmt_add_board_hidden_column FROM @add_board_hidden_column_sql;
EXECUTE stmt_add_board_hidden_column;
DEALLOCATE PREPARE stmt_add_board_hidden_column;

SET @ensure_board_hidden_default_sql := IF(
    @board_table_name IS NOT NULL,
    CONCAT(
        'UPDATE `', @board_table_name, '` ',
        'SET is_hidden_by_report = b''0'' ',
        'WHERE is_hidden_by_report IS NULL'
    ),
    'SELECT 1'
);

PREPARE stmt_ensure_board_hidden_default FROM @ensure_board_hidden_default_sql;
EXECUTE stmt_ensure_board_hidden_default;
DEALLOCATE PREPARE stmt_ensure_board_hidden_default;

SET @has_notification_comment_column := (
    SELECT IF(
        @notification_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @notification_table_name
              AND column_name = 'comment_id'
        )
    )
);

SET @notification_comment_nullable := (
    SELECT IF(
        @notification_table_name IS NULL
        OR @has_notification_comment_column = 0,
        1,
        (
            SELECT IF(IS_NULLABLE = 'YES', 1, 0)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @notification_table_name
              AND column_name = 'comment_id'
            LIMIT 1
        )
    )
);

SET @alter_notification_comment_nullable_sql := IF(
    @notification_table_name IS NOT NULL
    AND @has_notification_comment_column = 1
    AND @notification_comment_nullable = 0,
    CONCAT(
        'ALTER TABLE `', @notification_table_name, '` ',
        'MODIFY COLUMN comment_id BIGINT NULL'
    ),
    'SELECT 1'
);

PREPARE stmt_alter_notification_comment_nullable FROM @alter_notification_comment_nullable_sql;
EXECUTE stmt_alter_notification_comment_nullable;
DEALLOCATE PREPARE stmt_alter_notification_comment_nullable;

SET @has_bookmark_folder_column := (
    SELECT IF(
        @bookmark_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @bookmark_table_name
              AND column_name = 'folder_name'
        )
    )
);

SET @add_bookmark_folder_column_sql := IF(
    @bookmark_table_name IS NOT NULL
    AND @has_bookmark_folder_column = 0,
    CONCAT(
        'ALTER TABLE `', @bookmark_table_name, '` ',
        'ADD COLUMN folder_name VARCHAR(60) NULL'
    ),
    'SELECT 1'
);

PREPARE stmt_add_bookmark_folder_column FROM @add_bookmark_folder_column_sql;
EXECUTE stmt_add_bookmark_folder_column;
DEALLOCATE PREPARE stmt_add_bookmark_folder_column;

SET @has_bookmark_tag_column := (
    SELECT IF(
        @bookmark_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @bookmark_table_name
              AND column_name = 'tag_name'
        )
    )
);

SET @add_bookmark_tag_column_sql := IF(
    @bookmark_table_name IS NOT NULL
    AND @has_bookmark_tag_column = 0,
    CONCAT(
        'ALTER TABLE `', @bookmark_table_name, '` ',
        'ADD COLUMN tag_name VARCHAR(100) NULL'
    ),
    'SELECT 1'
);

PREPARE stmt_add_bookmark_tag_column FROM @add_bookmark_tag_column_sql;
EXECUTE stmt_add_bookmark_tag_column;
DEALLOCATE PREPARE stmt_add_bookmark_tag_column;

SET @recipe_review_table_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'recipe_review'
);

SET @create_recipe_review_table_sql := IF(
    @recipe_review_table_exists = 0
    AND @account_table_name IS NOT NULL
    AND @recipe_info_table_name IS NOT NULL,
    CONCAT(
        'CREATE TABLE recipe_review (',
        'id BIGINT NOT NULL AUTO_INCREMENT, ',
        'user_id BIGINT NOT NULL, ',
        'recipe_row_num BIGINT NOT NULL, ',
        'rating TINYINT NOT NULL, ',
        'contents VARCHAR(500) NULL, ',
        'regTime DATETIME(6) NULL, ',
        'updateTime DATETIME(6) NULL, ',
        'PRIMARY KEY (id), ',
        'CONSTRAINT uk_recipe_review_user_recipe UNIQUE (user_id, recipe_row_num), ',
        'INDEX idx_recipe_review_recipe_regtime (recipe_row_num, regTime), ',
        'INDEX idx_recipe_review_recipe_rating (recipe_row_num, rating), ',
        'INDEX idx_recipe_review_user_regtime (user_id, regTime), ',
        'CONSTRAINT fk_recipe_review_user FOREIGN KEY (user_id) REFERENCES `', @account_table_name, '` (id) ON DELETE CASCADE, ',
        'CONSTRAINT fk_recipe_review_recipe FOREIGN KEY (recipe_row_num) REFERENCES `', @recipe_info_table_name, '` (row_NUM) ON DELETE CASCADE',
        ')'
    ),
    'SELECT 1'
);

PREPARE stmt_create_recipe_review_table FROM @create_recipe_review_table_sql;
EXECUTE stmt_create_recipe_review_table;
DEALLOCATE PREPARE stmt_create_recipe_review_table;

SET @user_activity_table_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'user_activity_log'
);

SET @create_user_activity_table_sql := IF(
    @user_activity_table_exists = 0
    AND @account_table_name IS NOT NULL,
    CONCAT(
        'CREATE TABLE user_activity_log (',
        'id BIGINT NOT NULL AUTO_INCREMENT, ',
        'user_id BIGINT NOT NULL, ',
        'action VARCHAR(80) NOT NULL, ',
        'detail VARCHAR(500) NULL, ',
        'regTime DATETIME(6) NULL, ',
        'updateTime DATETIME(6) NULL, ',
        'PRIMARY KEY (id), ',
        'INDEX idx_user_activity_user_regtime (user_id, regTime), ',
        'INDEX idx_user_activity_action_regtime (action, regTime), ',
        'CONSTRAINT fk_user_activity_user FOREIGN KEY (user_id) REFERENCES `', @account_table_name, '` (id) ON DELETE CASCADE',
        ')'
    ),
    'SELECT 1'
);

PREPARE stmt_create_user_activity_table FROM @create_user_activity_table_sql;
EXECUTE stmt_create_user_activity_table;
DEALLOCATE PREPARE stmt_create_user_activity_table;
