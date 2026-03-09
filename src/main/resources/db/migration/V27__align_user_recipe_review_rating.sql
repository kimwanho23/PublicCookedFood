SET @user_recipe_review_rating_type := (
    SELECT LOWER(column_type)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'user_recipe_review'
      AND column_name = 'rating'
    LIMIT 1
);

SET @align_user_recipe_review_rating_sql := IF(
    @user_recipe_review_rating_type IS NULL OR @user_recipe_review_rating_type LIKE 'int%',
    'SELECT 1',
    'ALTER TABLE user_recipe_review MODIFY COLUMN rating INT NOT NULL'
);

PREPARE stmt_align_user_recipe_review_rating FROM @align_user_recipe_review_rating_sql;
EXECUTE stmt_align_user_recipe_review_rating;
DEALLOCATE PREPARE stmt_align_user_recipe_review_rating;
