SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'comments'
              AND COLUMN_NAME = 'root_parent_id'
        ),
        'SELECT 1',
        'ALTER TABLE comments ADD COLUMN root_parent_id BIGINT NULL AFTER parent_id'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'comments'
              AND COLUMN_NAME = 'depth'
        ),
        'SELECT 1',
        'ALTER TABLE comments ADD COLUMN depth INT NOT NULL DEFAULT 0 AFTER root_parent_id'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'comments'
              AND COLUMN_NAME = 'comment_path'
        ),
        'SELECT 1',
        'ALTER TABLE comments ADD COLUMN comment_path VARCHAR(2048) CHARACTER SET ascii COLLATE ascii_bin NULL AFTER depth'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE comments
    MODIFY COLUMN comment_path VARCHAR(2048) CHARACTER SET ascii COLLATE ascii_bin NULL;

WITH RECURSIVE comment_tree AS (
    SELECT c.id,
           c.id AS root_parent_id,
           0 AS depth,
           CAST(LPAD(CAST(c.id AS CHAR), 19, '0') AS CHAR(2048)) AS comment_path
    FROM comments c
    WHERE c.parent_id IS NULL

    UNION ALL

    SELECT child.id,
           comment_tree.root_parent_id,
           comment_tree.depth + 1,
           CAST(CONCAT(comment_tree.comment_path, '/', LPAD(CAST(child.id AS CHAR), 19, '0')) AS CHAR(2048))
    FROM comments child
    JOIN comment_tree ON child.parent_id = comment_tree.id
)
UPDATE comments c
JOIN comment_tree ON comment_tree.id = c.id
SET c.root_parent_id = comment_tree.root_parent_id,
    c.depth = comment_tree.depth,
    c.comment_path = comment_tree.comment_path;

UPDATE comments
SET root_parent_id = id,
    depth = 0,
    comment_path = LPAD(CAST(id AS CHAR), 19, '0')
WHERE root_parent_id IS NULL OR comment_path IS NULL;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'comments'
              AND INDEX_NAME = 'idx_comments_post_root_reg_time'
        ),
        'SELECT 1',
        'CREATE INDEX idx_comments_post_root_reg_time ON comments (post_id, root_parent_id, regTime)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'comments'
              AND INDEX_NAME = 'idx_comments_post_root_depth_reg_time'
        ),
        'SELECT 1',
        'CREATE INDEX idx_comments_post_root_depth_reg_time ON comments (post_id, root_parent_id, depth, regTime)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'comments'
              AND INDEX_NAME = 'idx_comments_post_root_comment_path'
        ),
        'SELECT 1',
        'CREATE INDEX idx_comments_post_root_comment_path ON comments (post_id, root_parent_id, comment_path)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
