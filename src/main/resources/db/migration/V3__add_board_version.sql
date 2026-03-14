ALTER TABLE board
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER section_id;

INSERT INTO board_stats (board_id, total_views, total_likes, total_comments, score, updated_at)
SELECT b.id,
       COALESCE(b.views, 0),
       COALESCE(b.likeCount, 0),
       COALESCE(b.commentCount, 0),
       0.000000,
       CURRENT_TIMESTAMP(6)
FROM board b
LEFT JOIN board_stats s ON s.board_id = b.id
WHERE s.board_id IS NULL;

ALTER TABLE board
    DROP INDEX idx_board_state_like_count,
    DROP INDEX idx_board_state_hidden_like_count_reg_time,
    DROP COLUMN views,
    DROP COLUMN likeCount,
    DROP COLUMN commentCount;
