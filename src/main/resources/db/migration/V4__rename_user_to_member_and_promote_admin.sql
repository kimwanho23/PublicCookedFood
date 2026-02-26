-- Rename reserved table name `user` to `member` and promote a seed admin account.
-- This script is idempotent and safe to run repeatedly.

SET @legacy_user_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'user'
    LIMIT 1
);

SET @member_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'member'
    LIMIT 1
);

SET @rename_user_to_member_sql := IF(
    @legacy_user_table_name IS NOT NULL
    AND @member_table_name IS NULL,
    CONCAT(
        'RENAME TABLE `', @legacy_user_table_name, '` TO `member`'
    ),
    'SELECT 1'
);

PREPARE stmt_rename_user_to_member FROM @rename_user_to_member_sql;
EXECUTE stmt_rename_user_to_member;
DEALLOCATE PREPARE stmt_rename_user_to_member;

SET @member_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'member'
    LIMIT 1
);

SET @member_has_email_column := (
    SELECT IF(
        @member_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @member_table_name
              AND column_name = 'email'
        )
    )
);

SET @member_has_authority_column := (
    SELECT IF(
        @member_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @member_table_name
              AND column_name = 'authority'
        )
    )
);

SET @member_authority_column_type := (
    SELECT IF(
        @member_table_name IS NULL,
        '',
        COALESCE(
            (
                SELECT column_type
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = @member_table_name
                  AND column_name = 'authority'
                LIMIT 1
            ),
            ''
        )
    )
);

SET @member_authority_data_type := (
    SELECT IF(
        @member_table_name IS NULL,
        '',
        COALESCE(
            (
                SELECT data_type
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = @member_table_name
                  AND column_name = 'authority'
                LIMIT 1
            ),
            ''
        )
    )
);

SET @member_authority_char_max_len := (
    SELECT IF(
        @member_table_name IS NULL,
        0,
        COALESCE(
            (
                SELECT character_maximum_length
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = @member_table_name
                  AND column_name = 'authority'
                LIMIT 1
            ),
            0
        )
    )
);

SET @promote_authority_value := IF(
    LOCATE('ROLE_', UPPER(@member_authority_column_type)) > 0,
    'ROLE_ADMIN',
    'ADMIN'
);

SET @authority_enum_missing_promote_value := (
    @member_table_name IS NOT NULL
    AND @member_has_authority_column > 0
    AND LOWER(@member_authority_data_type) = 'enum'
    AND LOCATE(CONCAT('''', UPPER(@promote_authority_value), ''''), UPPER(@member_authority_column_type)) = 0
);

SET @expand_authority_enum_sql := IF(
    @authority_enum_missing_promote_value,
    CONCAT(
        'ALTER TABLE `', @member_table_name, '` ',
        'MODIFY COLUMN authority ',
        SUBSTRING(@member_authority_column_type, 1, CHAR_LENGTH(@member_authority_column_type) - 1),
        ',''', @promote_authority_value, ''')'
    ),
    'SELECT 1'
);

PREPARE stmt_expand_authority_enum FROM @expand_authority_enum_sql;
EXECUTE stmt_expand_authority_enum;
DEALLOCATE PREPARE stmt_expand_authority_enum;

SET @authority_needs_varchar_expand := (
    @member_table_name IS NOT NULL
    AND @member_has_authority_column > 0
    AND LOWER(@member_authority_data_type) IN ('varchar', 'char')
    AND @member_authority_char_max_len > 0
    AND @member_authority_char_max_len < CHAR_LENGTH(@promote_authority_value)
);

SET @expand_authority_varchar_sql := IF(
    @authority_needs_varchar_expand,
    CONCAT(
        'ALTER TABLE `', @member_table_name, '` ',
        'MODIFY COLUMN authority VARCHAR(50)'
    ),
    'SELECT 1'
);

PREPARE stmt_expand_authority_varchar FROM @expand_authority_varchar_sql;
EXECUTE stmt_expand_authority_varchar;
DEALLOCATE PREPARE stmt_expand_authority_varchar;

SET @promote_test_admin_sql := IF(
    @member_table_name IS NOT NULL
    AND @member_has_email_column > 0
    AND @member_has_authority_column > 0,
    CONCAT(
        'UPDATE `', @member_table_name, '` ',
        'SET authority = ''', @promote_authority_value, ''' ',
        'WHERE LOWER(email) = ''test@gmail.com'''
    ),
    'SELECT 1'
);

PREPARE stmt_promote_test_admin FROM @promote_test_admin_sql;
EXECUTE stmt_promote_test_admin;
DEALLOCATE PREPARE stmt_promote_test_admin;
