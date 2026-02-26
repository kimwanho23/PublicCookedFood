-- Promote readerc866@gmail.com to admin role.
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

SET @account_has_email_column := (
    SELECT IF(
        @account_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @account_table_name
              AND column_name = 'email'
        )
    )
);

SET @account_has_authority_column := (
    SELECT IF(
        @account_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @account_table_name
              AND column_name = 'authority'
        )
    )
);

SET @account_authority_column_type := (
    SELECT IF(
        @account_table_name IS NULL,
        '',
        COALESCE(
            (
                SELECT column_type
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = @account_table_name
                  AND column_name = 'authority'
                LIMIT 1
            ),
            ''
        )
    )
);

SET @account_authority_data_type := (
    SELECT IF(
        @account_table_name IS NULL,
        '',
        COALESCE(
            (
                SELECT data_type
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = @account_table_name
                  AND column_name = 'authority'
                LIMIT 1
            ),
            ''
        )
    )
);

SET @account_authority_char_max_len := (
    SELECT IF(
        @account_table_name IS NULL,
        0,
        COALESCE(
            (
                SELECT character_maximum_length
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = @account_table_name
                  AND column_name = 'authority'
                LIMIT 1
            ),
            0
        )
    )
);

SET @promote_authority_value := IF(
    LOCATE('ROLE_', UPPER(@account_authority_column_type)) > 0,
    'ROLE_ADMIN',
    'ADMIN'
);

SET @authority_enum_missing_promote_value := (
    @account_table_name IS NOT NULL
    AND @account_has_authority_column > 0
    AND LOWER(@account_authority_data_type) = 'enum'
    AND LOCATE(CONCAT('''', UPPER(@promote_authority_value), ''''), UPPER(@account_authority_column_type)) = 0
);

SET @expand_authority_enum_sql := IF(
    @authority_enum_missing_promote_value,
    CONCAT(
        'ALTER TABLE `', @account_table_name, '` ',
        'MODIFY COLUMN authority ',
        SUBSTRING(@account_authority_column_type, 1, CHAR_LENGTH(@account_authority_column_type) - 1),
        ',''', @promote_authority_value, ''')'
    ),
    'SELECT 1'
);

PREPARE stmt_expand_authority_enum FROM @expand_authority_enum_sql;
EXECUTE stmt_expand_authority_enum;
DEALLOCATE PREPARE stmt_expand_authority_enum;

SET @authority_needs_varchar_expand := (
    @account_table_name IS NOT NULL
    AND @account_has_authority_column > 0
    AND LOWER(@account_authority_data_type) IN ('varchar', 'char')
    AND @account_authority_char_max_len > 0
    AND @account_authority_char_max_len < CHAR_LENGTH(@promote_authority_value)
);

SET @expand_authority_varchar_sql := IF(
    @authority_needs_varchar_expand,
    CONCAT(
        'ALTER TABLE `', @account_table_name, '` ',
        'MODIFY COLUMN authority VARCHAR(50)'
    ),
    'SELECT 1'
);

PREPARE stmt_expand_authority_varchar FROM @expand_authority_varchar_sql;
EXECUTE stmt_expand_authority_varchar;
DEALLOCATE PREPARE stmt_expand_authority_varchar;

SET @promote_readerc866_admin_sql := IF(
    @account_table_name IS NOT NULL
    AND @account_has_email_column > 0
    AND @account_has_authority_column > 0,
    CONCAT(
        'UPDATE `', @account_table_name, '` ',
        'SET authority = ''', @promote_authority_value, ''' ',
        'WHERE LOWER(email) = ''readerc866@gmail.com'''
    ),
    'SELECT 1'
);

PREPARE stmt_promote_readerc866_admin FROM @promote_readerc866_admin_sql;
EXECUTE stmt_promote_readerc866_admin;
DEALLOCATE PREPARE stmt_promote_readerc866_admin;
