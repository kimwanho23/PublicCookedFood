-- Extend member profile fields used by signup/profile/account-recovery.
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

SET @has_phone_number := (
    SELECT IF(
        @account_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @account_table_name
              AND column_name = 'phoneNumber'
        )
    )
);

SET @add_phone_number_sql := IF(
    @account_table_name IS NOT NULL
    AND @has_phone_number = 0,
    CONCAT(
        'ALTER TABLE `', @account_table_name, '` ',
        'ADD COLUMN phoneNumber VARCHAR(30) NULL'
    ),
    'SELECT 1'
);

PREPARE stmt_add_phone_number FROM @add_phone_number_sql;
EXECUTE stmt_add_phone_number;
DEALLOCATE PREPARE stmt_add_phone_number;

SET @has_birth_date := (
    SELECT IF(
        @account_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @account_table_name
              AND column_name = 'birthDate'
        )
    )
);

SET @add_birth_date_sql := IF(
    @account_table_name IS NOT NULL
    AND @has_birth_date = 0,
    CONCAT(
        'ALTER TABLE `', @account_table_name, '` ',
        'ADD COLUMN birthDate DATE NULL'
    ),
    'SELECT 1'
);

PREPARE stmt_add_birth_date FROM @add_birth_date_sql;
EXECUTE stmt_add_birth_date;
DEALLOCATE PREPARE stmt_add_birth_date;

SET @has_gender := (
    SELECT IF(
        @account_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @account_table_name
              AND column_name = 'gender'
        )
    )
);

SET @add_gender_sql := IF(
    @account_table_name IS NOT NULL
    AND @has_gender = 0,
    CONCAT(
        'ALTER TABLE `', @account_table_name, '` ',
        'ADD COLUMN gender VARCHAR(20) NULL'
    ),
    'SELECT 1'
);

PREPARE stmt_add_gender FROM @add_gender_sql;
EXECUTE stmt_add_gender;
DEALLOCATE PREPARE stmt_add_gender;

SET @has_address := (
    SELECT IF(
        @account_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @account_table_name
              AND column_name = 'address'
        )
    )
);

SET @add_address_sql := IF(
    @account_table_name IS NOT NULL
    AND @has_address = 0,
    CONCAT(
        'ALTER TABLE `', @account_table_name, '` ',
        'ADD COLUMN address VARCHAR(120) NULL'
    ),
    'SELECT 1'
);

PREPARE stmt_add_address FROM @add_address_sql;
EXECUTE stmt_add_address;
DEALLOCATE PREPARE stmt_add_address;

SET @has_address_detail := (
    SELECT IF(
        @account_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @account_table_name
              AND column_name = 'addressDetail'
        )
    )
);

SET @add_address_detail_sql := IF(
    @account_table_name IS NOT NULL
    AND @has_address_detail = 0,
    CONCAT(
        'ALTER TABLE `', @account_table_name, '` ',
        'ADD COLUMN addressDetail VARCHAR(120) NULL'
    ),
    'SELECT 1'
);

PREPARE stmt_add_address_detail FROM @add_address_detail_sql;
EXECUTE stmt_add_address_detail;
DEALLOCATE PREPARE stmt_add_address_detail;

SET @has_profile_image_url := (
    SELECT IF(
        @account_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = @account_table_name
              AND column_name = 'profileImageUrl'
        )
    )
);

SET @add_profile_image_url_sql := IF(
    @account_table_name IS NOT NULL
    AND @has_profile_image_url = 0,
    CONCAT(
        'ALTER TABLE `', @account_table_name, '` ',
        'ADD COLUMN profileImageUrl VARCHAR(500) NULL'
    ),
    'SELECT 1'
);

PREPARE stmt_add_profile_image_url FROM @add_profile_image_url_sql;
EXECUTE stmt_add_profile_image_url;
DEALLOCATE PREPARE stmt_add_profile_image_url;

SET @phone_number_index_exists := (
    SELECT IF(
        @account_table_name IS NULL,
        0,
        (
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = @account_table_name
              AND index_name = 'idx_member_phone_number'
        )
    )
);

SET @add_phone_number_index_sql := IF(
    @account_table_name IS NOT NULL
    AND @phone_number_index_exists = 0,
    CONCAT(
        'ALTER TABLE `', @account_table_name, '` ',
        'ADD INDEX idx_member_phone_number (phoneNumber)'
    ),
    'SELECT 1'
);

PREPARE stmt_add_phone_number_index FROM @add_phone_number_index_sql;
EXECUTE stmt_add_phone_number_index;
DEALLOCATE PREPARE stmt_add_phone_number_index;
