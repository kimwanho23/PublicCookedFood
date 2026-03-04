-- Search query index tuning for board/recipe listing.
-- This script is idempotent and safe to run repeatedly.

SET @board_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'board'
    LIMIT 1
);

SET @recipe_info_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'recipe_info'
    LIMIT 1
);

SET @board_reg_time_column := (
    SELECT column_name
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = @board_table_name
      AND LOWER(column_name) IN ('regtime', 'reg_time')
    ORDER BY CASE WHEN column_name = 'regTime' THEN 0 ELSE 1 END
    LIMIT 1
);

SET @board_section_column := (
    SELECT column_name
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = @board_table_name
      AND LOWER(column_name) = 'section_id'
    LIMIT 1
);

SET @board_like_count_column := (
    SELECT column_name
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = @board_table_name
      AND LOWER(column_name) IN ('likecount', 'like_count')
    ORDER BY CASE WHEN column_name = 'likeCount' THEN 0 ELSE 1 END
    LIMIT 1
);

SET @board_has_hidden_by_report := (
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

SET @idx_board_state_hidden_reg_time_exists := (
    SELECT IF(
        @board_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = @board_table_name
              AND index_name = 'idx_board_state_hidden_reg_time'
        )
    )
);

SET @add_idx_board_state_hidden_reg_time_sql := IF(
    @board_table_name IS NOT NULL
    AND @board_has_hidden_by_report > 0
    AND @board_reg_time_column IS NOT NULL
    AND @idx_board_state_hidden_reg_time_exists = 0,
    CONCAT(
        'ALTER TABLE `', @board_table_name, '` ',
        'ADD INDEX idx_board_state_hidden_reg_time (`state`, `is_hidden_by_report`, `', @board_reg_time_column, '`)'
    ),
    'SELECT 1'
);

PREPARE stmt_add_idx_board_state_hidden_reg_time FROM @add_idx_board_state_hidden_reg_time_sql;
EXECUTE stmt_add_idx_board_state_hidden_reg_time;
DEALLOCATE PREPARE stmt_add_idx_board_state_hidden_reg_time;

SET @idx_board_state_hidden_section_reg_time_exists := (
    SELECT IF(
        @board_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = @board_table_name
              AND index_name = 'idx_board_state_hidden_section_reg_time'
        )
    )
);

SET @add_idx_board_state_hidden_section_reg_time_sql := IF(
    @board_table_name IS NOT NULL
    AND @board_has_hidden_by_report > 0
    AND @board_section_column IS NOT NULL
    AND @board_reg_time_column IS NOT NULL
    AND @idx_board_state_hidden_section_reg_time_exists = 0,
    CONCAT(
        'ALTER TABLE `', @board_table_name, '` ',
        'ADD INDEX idx_board_state_hidden_section_reg_time (`state`, `is_hidden_by_report`, `', @board_section_column, '`, `', @board_reg_time_column, '`)'
    ),
    'SELECT 1'
);

PREPARE stmt_add_idx_board_state_hidden_section_reg_time FROM @add_idx_board_state_hidden_section_reg_time_sql;
EXECUTE stmt_add_idx_board_state_hidden_section_reg_time;
DEALLOCATE PREPARE stmt_add_idx_board_state_hidden_section_reg_time;

SET @idx_board_state_hidden_like_count_reg_time_exists := (
    SELECT IF(
        @board_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = @board_table_name
              AND index_name = 'idx_board_state_hidden_like_count_reg_time'
        )
    )
);

SET @add_idx_board_state_hidden_like_count_reg_time_sql := IF(
    @board_table_name IS NOT NULL
    AND @board_has_hidden_by_report > 0
    AND @board_like_count_column IS NOT NULL
    AND @board_reg_time_column IS NOT NULL
    AND @idx_board_state_hidden_like_count_reg_time_exists = 0,
    CONCAT(
        'ALTER TABLE `', @board_table_name, '` ',
        'ADD INDEX idx_board_state_hidden_like_count_reg_time (`state`, `is_hidden_by_report`, `', @board_like_count_column, '`, `', @board_reg_time_column, '`)'
    ),
    'SELECT 1'
);

PREPARE stmt_add_idx_board_state_hidden_like_count_reg_time FROM @add_idx_board_state_hidden_like_count_reg_time_sql;
EXECUTE stmt_add_idx_board_state_hidden_like_count_reg_time;
DEALLOCATE PREPARE stmt_add_idx_board_state_hidden_like_count_reg_time;

SET @recipe_name_column := (
    SELECT column_name
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = @recipe_info_table_name
      AND LOWER(column_name) = 'recipe_nm_ko'
    LIMIT 1
);

SET @recipe_row_num_column := (
    SELECT column_name
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = @recipe_info_table_name
      AND LOWER(column_name) = 'row_num'
    LIMIT 1
);

SET @idx_recipe_info_recipe_nm_ko_row_num_exists := (
    SELECT IF(
        @recipe_info_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = @recipe_info_table_name
              AND index_name = 'idx_recipe_info_recipe_nm_ko_row_num'
        )
    )
);

SET @add_idx_recipe_info_recipe_nm_ko_row_num_sql := IF(
    @recipe_info_table_name IS NOT NULL
    AND @recipe_name_column IS NOT NULL
    AND @recipe_row_num_column IS NOT NULL
    AND @idx_recipe_info_recipe_nm_ko_row_num_exists = 0,
    CONCAT(
        'ALTER TABLE `', @recipe_info_table_name, '` ',
        'ADD INDEX idx_recipe_info_recipe_nm_ko_row_num (`', @recipe_name_column, '`, `', @recipe_row_num_column, '`)'
    ),
    'SELECT 1'
);

PREPARE stmt_add_idx_recipe_info_recipe_nm_ko_row_num FROM @add_idx_recipe_info_recipe_nm_ko_row_num_sql;
EXECUTE stmt_add_idx_recipe_info_recipe_nm_ko_row_num;
DEALLOCATE PREPARE stmt_add_idx_recipe_info_recipe_nm_ko_row_num;
