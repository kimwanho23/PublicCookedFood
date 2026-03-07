-- Enforce unique member nickname semantics for mentions.
-- This script is idempotent and fails fast when blank, invalid-format,
-- or duplicate nicknames already exist.

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

SET @has_name_column := (
    SELECT IF(
        @account_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @account_table_name
              AND column_name = 'name'
        )
    )
);

SET @blank_nickname_count := 0;

SET @count_blank_nickname_sql := IF(
    @account_table_name IS NOT NULL
    AND @has_name_column > 0,
    CONCAT(
        'SELECT COUNT(*) INTO @blank_nickname_count ',
        'FROM `', @account_table_name, '` ',
        'WHERE name IS NULL OR TRIM(name) = '''''
    ),
    'SELECT 0 INTO @blank_nickname_count'
);

PREPARE stmt_count_blank_nickname FROM @count_blank_nickname_sql;
EXECUTE stmt_count_blank_nickname;
DEALLOCATE PREPARE stmt_count_blank_nickname;

SET @assert_blank_nickname_sql := IF(
    @blank_nickname_count > 0,
    'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''member.name에 빈 닉네임 데이터가 있어 unique 제약을 적용할 수 없습니다. 빈 닉네임을 먼저 정리해주세요.''',
    'SELECT 1'
);

PREPARE stmt_assert_blank_nickname FROM @assert_blank_nickname_sql;
EXECUTE stmt_assert_blank_nickname;
DEALLOCATE PREPARE stmt_assert_blank_nickname;

SET @invalid_nickname_count := 0;

SET @count_invalid_nickname_sql := IF(
    @account_table_name IS NOT NULL
    AND @has_name_column > 0,
    CONCAT(
        'SELECT COUNT(*) INTO @invalid_nickname_count ',
        'FROM `', @account_table_name, '` ',
        'WHERE name IS NOT NULL ',
        'AND TRIM(name) <> '''' ',
        'AND NOT REGEXP_LIKE(TRIM(name), ''^[\\\\p{L}\\\\p{N}_-]{2,20}$'')'
    ),
    'SELECT 0 INTO @invalid_nickname_count'
);

PREPARE stmt_count_invalid_nickname FROM @count_invalid_nickname_sql;
EXECUTE stmt_count_invalid_nickname;
DEALLOCATE PREPARE stmt_count_invalid_nickname;

SET @assert_invalid_nickname_sql := IF(
    @invalid_nickname_count > 0,
    'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''member.name에 닉네임 규칙(2~20자의 한글/영문/숫자/_/-)에 맞지 않는 데이터가 있어 unique 제약을 적용할 수 없습니다. 운영 데이터를 먼저 정리해주세요.''',
    'SELECT 1'
);

PREPARE stmt_assert_invalid_nickname FROM @assert_invalid_nickname_sql;
EXECUTE stmt_assert_invalid_nickname;
DEALLOCATE PREPARE stmt_assert_invalid_nickname;

SET @duplicate_normalized_nickname_count := 0;

SET @count_duplicate_normalized_nickname_sql := IF(
    @account_table_name IS NOT NULL
    AND @has_name_column > 0,
    CONCAT(
        'SELECT COUNT(*) INTO @duplicate_normalized_nickname_count ',
        'FROM (',
        'SELECT LOWER(TRIM(name)) AS normalized_name ',
        'FROM `', @account_table_name, '` ',
        'WHERE name IS NOT NULL AND TRIM(name) <> '''' ',
        'GROUP BY LOWER(TRIM(name)) ',
        'HAVING COUNT(*) > 1',
        ') duplicate_names'
    ),
    'SELECT 0 INTO @duplicate_normalized_nickname_count'
);

PREPARE stmt_count_duplicate_normalized_nickname FROM @count_duplicate_normalized_nickname_sql;
EXECUTE stmt_count_duplicate_normalized_nickname;
DEALLOCATE PREPARE stmt_count_duplicate_normalized_nickname;

SET @assert_duplicate_normalized_nickname_sql := IF(
    @duplicate_normalized_nickname_count > 0,
    'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''member.name에 중복 닉네임 데이터가 있어 unique 제약을 적용할 수 없습니다. 중복 닉네임을 먼저 정리해주세요.''',
    'SELECT 1'
);

PREPARE stmt_assert_duplicate_normalized_nickname FROM @assert_duplicate_normalized_nickname_sql;
EXECUTE stmt_assert_duplicate_normalized_nickname;
DEALLOCATE PREPARE stmt_assert_duplicate_normalized_nickname;

SET @trim_member_name_sql := IF(
    @account_table_name IS NOT NULL
    AND @has_name_column > 0,
    CONCAT(
        'UPDATE `', @account_table_name, '` ',
        'SET name = TRIM(name) ',
        'WHERE name IS NOT NULL AND name <> TRIM(name)'
    ),
    'SELECT 1'
);

PREPARE stmt_trim_member_name FROM @trim_member_name_sql;
EXECUTE stmt_trim_member_name;
DEALLOCATE PREPARE stmt_trim_member_name;

SET @nickname_unique_index_exists := (
    SELECT IF(
        @account_table_name IS NULL,
        0,
        (
            SELECT COUNT(DISTINCT index_name)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = @account_table_name
              AND column_name = 'name'
              AND non_unique = 0
        )
    )
);

SET @add_nickname_unique_sql := IF(
    @account_table_name IS NOT NULL
    AND @has_name_column > 0
    AND @nickname_unique_index_exists = 0,
    CONCAT(
        'ALTER TABLE `', @account_table_name, '` ',
        'ADD CONSTRAINT uk_member_name UNIQUE (name)'
    ),
    'SELECT 1'
);

PREPARE stmt_add_nickname_unique FROM @add_nickname_unique_sql;
EXECUTE stmt_add_nickname_unique;
DEALLOCATE PREPARE stmt_add_nickname_unique;
