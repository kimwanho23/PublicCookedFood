-- Notification table for board comment/reply alerts.
-- This script is idempotent and safe to run repeatedly.

SET @user_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'user'
    LIMIT 1
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

SET @notification_table_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'notification'
);

SET @create_notification_table_sql := IF(
    @user_table_name IS NOT NULL
    AND @board_table_name IS NOT NULL
    AND @comments_table_name IS NOT NULL
    AND @notification_table_exists = 0,
    CONCAT(
        'CREATE TABLE notification (',
        'id BIGINT NOT NULL AUTO_INCREMENT, ',
        'receiver_id BIGINT NOT NULL, ',
        'actor_id BIGINT NOT NULL, ',
        'board_id BIGINT NOT NULL, ',
        'comment_id BIGINT NOT NULL, ',
        'type VARCHAR(30) NOT NULL, ',
        'content_preview VARCHAR(255) NULL, ',
        'is_read BIT(1) NOT NULL DEFAULT b''0'', ',
        'read_time DATETIME(6) NULL, ',
        'regTime DATETIME(6) NULL, ',
        'updateTime DATETIME(6) NULL, ',
        'PRIMARY KEY (id), ',
        'INDEX idx_notification_receiver_read_regtime (receiver_id, is_read, regTime), ',
        'INDEX idx_notification_receiver_regtime (receiver_id, regTime), ',
        'INDEX idx_notification_board_comment (board_id, comment_id), ',
        'CONSTRAINT fk_notification_receiver FOREIGN KEY (receiver_id) REFERENCES `', @user_table_name, '` (id) ON DELETE CASCADE, ',
        'CONSTRAINT fk_notification_actor FOREIGN KEY (actor_id) REFERENCES `', @user_table_name, '` (id) ON DELETE CASCADE, ',
        'CONSTRAINT fk_notification_board FOREIGN KEY (board_id) REFERENCES `', @board_table_name, '` (id) ON DELETE CASCADE, ',
        'CONSTRAINT fk_notification_comment FOREIGN KEY (comment_id) REFERENCES `', @comments_table_name, '` (id) ON DELETE CASCADE',
        ')'
    ),
    'SELECT 1'
);

PREPARE stmt_create_notification_table FROM @create_notification_table_sql;
EXECUTE stmt_create_notification_table;
DEALLOCATE PREPARE stmt_create_notification_table;
