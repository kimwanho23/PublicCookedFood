-- Replace hardcoded admin promotion with configurable bootstrap admin policy.
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

SET @bootstrap_admin_email := LOWER(TRIM('${bootstrap_admin_email}'));
SET @hardcoded_admin_email := 'readerc866@gmail.com';
SET @admin_authority_value := IF(
    LOCATE('ROLE_', UPPER(@account_authority_column_type)) > 0,
    'ROLE_ADMIN',
    'ADMIN'
);
SET @user_authority_value := IF(
    LOCATE('ROLE_', UPPER(@account_authority_column_type)) > 0,
    'ROLE_USER',
    'USER'
);

-- 1) Explicit bootstrap admin email is configured -> promote that account.
SET @promote_bootstrap_admin_sql := IF(
    @account_table_name IS NOT NULL
    AND @account_has_email_column > 0
    AND @account_has_authority_column > 0
    AND @bootstrap_admin_email <> '',
    CONCAT(
        'UPDATE `', @account_table_name, '` ',
        'SET authority = ''', @admin_authority_value, ''' ',
        'WHERE LOWER(email) = ''', REPLACE(@bootstrap_admin_email, '''', ''''''), ''''
    ),
    'SELECT 1'
);

PREPARE stmt_promote_bootstrap_admin FROM @promote_bootstrap_admin_sql;
EXECUTE stmt_promote_bootstrap_admin;
DEALLOCATE PREPARE stmt_promote_bootstrap_admin;

-- 2) Explicit bootstrap admin differs from legacy hardcoded account -> demote hardcoded admin.
SET @demote_hardcoded_when_bootstrap_sql := IF(
    @account_table_name IS NOT NULL
    AND @account_has_email_column > 0
    AND @account_has_authority_column > 0
    AND @bootstrap_admin_email <> ''
    AND @bootstrap_admin_email <> @hardcoded_admin_email,
    CONCAT(
        'UPDATE `', @account_table_name, '` ',
        'SET authority = ''', @user_authority_value, ''' ',
        'WHERE LOWER(email) = ''', @hardcoded_admin_email, ''' ',
        'AND authority = ''', @admin_authority_value, ''''
    ),
    'SELECT 1'
);

PREPARE stmt_demote_hardcoded_when_bootstrap FROM @demote_hardcoded_when_bootstrap_sql;
EXECUTE stmt_demote_hardcoded_when_bootstrap;
DEALLOCATE PREPARE stmt_demote_hardcoded_when_bootstrap;

-- 3) No bootstrap admin configured -> demote hardcoded admin only when another admin exists.
SET @demote_hardcoded_when_empty_sql := IF(
    @account_table_name IS NOT NULL
    AND @account_has_email_column > 0
    AND @account_has_authority_column > 0
    AND @bootstrap_admin_email = '',
    CONCAT(
        'UPDATE `', @account_table_name, '` target ',
        'SET target.authority = ''', @user_authority_value, ''' ',
        'WHERE LOWER(target.email) = ''', @hardcoded_admin_email, ''' ',
        'AND target.authority = ''', @admin_authority_value, ''' ',
        'AND EXISTS (',
            'SELECT 1 FROM (',
                'SELECT id FROM `', @account_table_name, '` ',
                'WHERE authority = ''', @admin_authority_value, ''' ',
                'AND LOWER(email) <> ''', @hardcoded_admin_email, ''' ',
                'LIMIT 1',
            ') admin_guard',
        ')'
    ),
    'SELECT 1'
);

PREPARE stmt_demote_hardcoded_when_empty FROM @demote_hardcoded_when_empty_sql;
EXECUTE stmt_demote_hardcoded_when_empty;
DEALLOCATE PREPARE stmt_demote_hardcoded_when_empty;
