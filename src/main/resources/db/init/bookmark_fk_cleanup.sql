-- Ensure bookmark rows are valid before Hibernate tries to maintain FK/unique constraints.
-- This script is idempotent and safe to run repeatedly.

SET @bookmark_table_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'bookmark'
);

SET @bookmark_has_account_id := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bookmark'
      AND column_name = 'account_id'
);

SET @bookmark_has_email := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bookmark'
      AND column_name = 'email'
);

SET @bookmark_email_not_nullable := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bookmark'
      AND column_name = 'email'
      AND is_nullable = 'NO'
);

SET @bookmark_email_column_type := (
    SELECT column_type
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bookmark'
      AND column_name = 'email'
    LIMIT 1
);

SET @bookmark_has_recipe_id := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bookmark'
      AND column_name = 'recipe_ID'
);

SET @account_table_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'accounts'
);

SET @recipe_info_table_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'Recipe_INFO'
);

SET @board_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'board'
    LIMIT 1
);

SET @comments_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'comments'
    LIMIT 1
);

SET @likes_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'likes'
    LIMIT 1
);

SET @board_has_account_id := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = @board_table_name
      AND column_name = 'account_id'
);

SET @comments_has_account_id := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = @comments_table_name
      AND column_name = 'account_id'
);

SET @comments_has_post_id := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = @comments_table_name
      AND column_name = 'post_id'
);

SET @comments_has_parent_id := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = @comments_table_name
      AND column_name = 'parent_id'
);

SET @likes_has_account_id := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = @likes_table_name
      AND column_name = 'account_id'
);

SET @likes_has_post_id := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = @likes_table_name
      AND column_name = 'post_id'
);

SET @images_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'images'
    LIMIT 1
);

SET @images_has_post_id := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = @images_table_name
      AND column_name = 'post_id'
);

SET @images_post_id_not_nullable := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = @images_table_name
      AND column_name = 'post_id'
      AND is_nullable = 'NO'
);

SET @images_post_id_column_type := (
    SELECT column_type
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = @images_table_name
      AND column_name = 'post_id'
    LIMIT 1
);

SET @board_image_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'board_image'
    LIMIT 1
);

SET @board_image_table_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'board_image'
);

-- 1) Legacy backfill: email -> account_id
SET @backfill_sql := IF(
    @bookmark_table_exists > 0
    AND @bookmark_has_account_id > 0
    AND @bookmark_has_email > 0
    AND @account_table_exists > 0,
    'UPDATE bookmark b JOIN `accounts` a ON a.email = b.email SET b.account_id = a.id WHERE b.account_id IS NULL AND b.email IS NOT NULL',
    'SELECT 1'
);
PREPARE stmt_backfill FROM @backfill_sql;
EXECUTE stmt_backfill;
DEALLOCATE PREPARE stmt_backfill;

-- 1-1) Legacy email column was NOT NULL in old schema; make it nullable so inserts(account_id, recipe_ID) work.
SET @make_email_nullable_sql := IF(
    @bookmark_table_exists > 0
    AND @bookmark_has_email > 0
    AND @bookmark_email_not_nullable > 0
    AND @bookmark_email_column_type IS NOT NULL,
    CONCAT('ALTER TABLE bookmark MODIFY COLUMN email ', @bookmark_email_column_type, ' NULL'),
    'SELECT 1'
);
PREPARE stmt_make_email_nullable FROM @make_email_nullable_sql;
EXECUTE stmt_make_email_nullable;
DEALLOCATE PREPARE stmt_make_email_nullable;

-- 2) Remove rows still missing account_id
SET @delete_null_user_sql := IF(
    @bookmark_table_exists > 0 AND @bookmark_has_account_id > 0,
    'DELETE FROM bookmark WHERE account_id IS NULL',
    'SELECT 1'
);
PREPARE stmt_delete_null_user FROM @delete_null_user_sql;
EXECUTE stmt_delete_null_user;
DEALLOCATE PREPARE stmt_delete_null_user;

-- 3) Remove rows referencing deleted/non-existing accounts
SET @delete_orphan_user_sql := IF(
    @bookmark_table_exists > 0
    AND @bookmark_has_account_id > 0
    AND @account_table_exists > 0,
    'DELETE b FROM bookmark b LEFT JOIN `accounts` a ON b.account_id = a.id WHERE a.id IS NULL',
    'SELECT 1'
);
PREPARE stmt_delete_orphan_user FROM @delete_orphan_user_sql;
EXECUTE stmt_delete_orphan_user;
DEALLOCATE PREPARE stmt_delete_orphan_user;

-- 4) Remove rows referencing non-existing recipes
SET @delete_orphan_recipe_sql := IF(
    @bookmark_table_exists > 0
    AND @bookmark_has_recipe_id > 0
    AND @recipe_info_table_exists > 0,
    'DELETE b FROM bookmark b LEFT JOIN Recipe_INFO r ON b.recipe_ID = r.row_NUM WHERE r.row_NUM IS NULL',
    'SELECT 1'
);
PREPARE stmt_delete_orphan_recipe FROM @delete_orphan_recipe_sql;
EXECUTE stmt_delete_orphan_recipe;
DEALLOCATE PREPARE stmt_delete_orphan_recipe;

-- 5) Remove duplicated rows before unique(account_id, recipe_ID) constraint checks
SET @delete_duplicate_sql := IF(
    @bookmark_table_exists > 0
    AND @bookmark_has_account_id > 0
    AND @bookmark_has_recipe_id > 0,
    'DELETE b1 FROM bookmark b1 JOIN bookmark b2 ON b1.account_id = b2.account_id AND b1.recipe_ID = b2.recipe_ID AND b1.id > b2.id',
    'SELECT 1'
);
PREPARE stmt_delete_duplicate FROM @delete_duplicate_sql;
EXECUTE stmt_delete_duplicate;
DEALLOCATE PREPARE stmt_delete_duplicate;

-- 6) Cleanup board rows pointing to missing accounts
SET @delete_orphan_board_user_sql := IF(
    @board_table_name IS NOT NULL
    AND @board_has_account_id > 0
    AND @account_table_exists > 0,
    CONCAT('DELETE b FROM `', @board_table_name, '` b LEFT JOIN `accounts` a ON b.account_id = a.id WHERE a.id IS NULL'),
    'SELECT 1'
);
PREPARE stmt_delete_orphan_board_user FROM @delete_orphan_board_user_sql;
EXECUTE stmt_delete_orphan_board_user;
DEALLOCATE PREPARE stmt_delete_orphan_board_user;

-- 7) Cleanup comments rows pointing to missing accounts/boards/parents
SET @delete_orphan_comments_user_sql := IF(
    @comments_table_name IS NOT NULL
    AND @comments_has_account_id > 0
    AND @account_table_exists > 0,
    CONCAT('DELETE c FROM `', @comments_table_name, '` c LEFT JOIN `accounts` a ON c.account_id = a.id WHERE a.id IS NULL'),
    'SELECT 1'
);
PREPARE stmt_delete_orphan_comments_user FROM @delete_orphan_comments_user_sql;
EXECUTE stmt_delete_orphan_comments_user;
DEALLOCATE PREPARE stmt_delete_orphan_comments_user;

SET @delete_orphan_comments_post_sql := IF(
    @comments_table_name IS NOT NULL
    AND @comments_has_post_id > 0
    AND @board_table_name IS NOT NULL,
    CONCAT('DELETE c FROM `', @comments_table_name, '` c LEFT JOIN `', @board_table_name, '` b ON c.post_id = b.id WHERE b.id IS NULL'),
    'SELECT 1'
);
PREPARE stmt_delete_orphan_comments_post FROM @delete_orphan_comments_post_sql;
EXECUTE stmt_delete_orphan_comments_post;
DEALLOCATE PREPARE stmt_delete_orphan_comments_post;

SET @delete_orphan_comments_parent_sql := IF(
    @comments_table_name IS NOT NULL
    AND @comments_has_parent_id > 0,
    CONCAT('DELETE c FROM `', @comments_table_name, '` c LEFT JOIN `', @comments_table_name, '` p ON c.parent_id = p.id WHERE c.parent_id IS NOT NULL AND p.id IS NULL'),
    'SELECT 1'
);
PREPARE stmt_delete_orphan_comments_parent FROM @delete_orphan_comments_parent_sql;
EXECUTE stmt_delete_orphan_comments_parent;
DEALLOCATE PREPARE stmt_delete_orphan_comments_parent;

-- 8) Cleanup likes rows pointing to missing accounts/boards
SET @delete_orphan_likes_user_sql := IF(
    @likes_table_name IS NOT NULL
    AND @likes_has_account_id > 0
    AND @account_table_exists > 0,
    CONCAT('DELETE l FROM `', @likes_table_name, '` l LEFT JOIN `accounts` a ON l.account_id = a.id WHERE a.id IS NULL'),
    'SELECT 1'
);
PREPARE stmt_delete_orphan_likes_user FROM @delete_orphan_likes_user_sql;
EXECUTE stmt_delete_orphan_likes_user;
DEALLOCATE PREPARE stmt_delete_orphan_likes_user;

SET @delete_orphan_likes_post_sql := IF(
    @likes_table_name IS NOT NULL
    AND @likes_has_post_id > 0
    AND @board_table_name IS NOT NULL,
    CONCAT('DELETE l FROM `', @likes_table_name, '` l LEFT JOIN `', @board_table_name, '` b ON l.post_id = b.id WHERE b.id IS NULL'),
    'SELECT 1'
);
PREPARE stmt_delete_orphan_likes_post FROM @delete_orphan_likes_post_sql;
EXECUTE stmt_delete_orphan_likes_post;
DEALLOCATE PREPARE stmt_delete_orphan_likes_post;

-- 9) TEMP 이미지 업로드를 위해 images.post_id는 NULL 허용이어야 한다.
SET @images_make_post_id_nullable_sql := IF(
    @images_table_name IS NOT NULL
    AND @images_has_post_id > 0
    AND @images_post_id_not_nullable > 0
    AND @images_post_id_column_type IS NOT NULL,
    CONCAT('ALTER TABLE `', @images_table_name, '` MODIFY COLUMN post_id ', @images_post_id_column_type, ' NULL'),
    'SELECT 1'
);
PREPARE stmt_images_make_post_id_nullable FROM @images_make_post_id_nullable_sql;
EXECUTE stmt_images_make_post_id_nullable;
DEALLOCATE PREPARE stmt_images_make_post_id_nullable;

-- 10) 게시글-이미지 연결 테이블(board_image) 생성
SET @create_board_image_table_sql := IF(
    @board_table_name IS NOT NULL
    AND @images_table_name IS NOT NULL
    AND @board_image_table_exists = 0,
    CONCAT(
        'CREATE TABLE board_image (',
        'id BIGINT NOT NULL AUTO_INCREMENT, ',
        'board_id BIGINT NOT NULL, ',
        'image_id BIGINT NOT NULL, ',
        'regTime DATETIME(6) NULL, ',
        'updateTime DATETIME(6) NULL, ',
        'PRIMARY KEY (id), ',
        'CONSTRAINT uk_board_image_board_id_image_id UNIQUE (board_id, image_id), ',
        'INDEX idx_board_image_board_id (board_id), ',
        'INDEX idx_board_image_image_id (image_id), ',
        'CONSTRAINT fk_board_image_board FOREIGN KEY (board_id) REFERENCES `', @board_table_name, '` (id) ON DELETE CASCADE, ',
        'CONSTRAINT fk_board_image_image FOREIGN KEY (image_id) REFERENCES `', @images_table_name, '` (id) ON DELETE CASCADE',
        ')'
    ),
    'SELECT 1'
);
PREPARE stmt_create_board_image_table FROM @create_board_image_table_sql;
EXECUTE stmt_create_board_image_table;
DEALLOCATE PREPARE stmt_create_board_image_table;

SET @board_image_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'board_image'
    LIMIT 1
);

-- 11) 레거시 images.post_id 데이터 -> board_image 백필
SET @backfill_board_image_sql := IF(
    @board_image_table_name IS NOT NULL
    AND @images_table_name IS NOT NULL
    AND @board_table_name IS NOT NULL
    AND @images_has_post_id > 0,
    CONCAT(
        'INSERT INTO `', @board_image_table_name, '` (board_id, image_id, regTime, updateTime) ',
        'SELECT i.post_id, i.id, COALESCE(i.regTime, NOW()), COALESCE(i.updateTime, NOW()) ',
        'FROM `', @images_table_name, '` i ',
        'JOIN `', @board_table_name, '` b ON b.id = i.post_id ',
        'LEFT JOIN `', @board_image_table_name, '` bi ON bi.board_id = i.post_id AND bi.image_id = i.id ',
        'WHERE i.post_id IS NOT NULL AND bi.id IS NULL'
    ),
    'SELECT 1'
);
PREPARE stmt_backfill_board_image FROM @backfill_board_image_sql;
EXECUTE stmt_backfill_board_image;
DEALLOCATE PREPARE stmt_backfill_board_image;

-- 12) board_image에 연결된 이미지는 ATTACHED 상태로 정합성 보정(DELETED 제외)
SET @sync_image_status_sql := IF(
    @board_image_table_name IS NOT NULL
    AND @images_table_name IS NOT NULL,
    CONCAT(
        'UPDATE `', @images_table_name, '` i ',
        'JOIN `', @board_image_table_name, '` bi ON bi.image_id = i.id ',
        'SET i.status = ''ATTACHED'' ',
        'WHERE i.status <> ''DELETED'''
    ),
    'SELECT 1'
);
PREPARE stmt_sync_image_status FROM @sync_image_status_sql;
EXECUTE stmt_sync_image_status;
DEALLOCATE PREPARE stmt_sync_image_status;
