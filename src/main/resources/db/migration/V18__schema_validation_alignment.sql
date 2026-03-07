-- Align schema objects with current JPA validation expectations.

SET @has_user_seq := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'user_seq'
);

SET @has_member_seq := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'member_seq'
);

SET @rename_user_seq_sql := IF(
    @has_member_seq = 0 AND @has_user_seq > 0,
    'RENAME TABLE user_seq TO member_seq',
    'SELECT 1'
);

PREPARE stmt_rename_user_seq FROM @rename_user_seq_sql;
EXECUTE stmt_rename_user_seq;
DEALLOCATE PREPARE stmt_rename_user_seq;

SET @create_member_seq_sql := IF(
    (
        SELECT COUNT(*)
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND LOWER(table_name) = 'member_seq'
    ) = 0,
    'CREATE TABLE member_seq (next_val BIGINT DEFAULT NULL)',
    'SELECT 1'
);

PREPARE stmt_create_member_seq FROM @create_member_seq_sql;
EXECUTE stmt_create_member_seq;
DEALLOCATE PREPARE stmt_create_member_seq;

INSERT INTO member_seq (next_val)
SELECT 1
WHERE NOT EXISTS (
    SELECT 1
    FROM member_seq
);

UPDATE member_seq
SET next_val = GREATEST(
    COALESCE(next_val, 1),
    COALESCE((SELECT MAX(id) + 1 FROM member), 1)
);

SET @recipe_review_rating_type := (
    SELECT LOWER(column_type)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'recipe_review'
      AND column_name = 'rating'
    LIMIT 1
);

SET @align_recipe_review_rating_sql := IF(
    @recipe_review_rating_type IS NULL OR @recipe_review_rating_type = 'int',
    'SELECT 1',
    'ALTER TABLE recipe_review MODIFY COLUMN rating INT NOT NULL'
);

PREPARE stmt_align_recipe_review_rating FROM @align_recipe_review_rating_sql;
EXECUTE stmt_align_recipe_review_rating;
DEALLOCATE PREPARE stmt_align_recipe_review_rating;
