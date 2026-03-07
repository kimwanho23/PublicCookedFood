-- Popular policy v1 baseline:
-- - aggregated stats tables (source of truth for ranking inputs)
-- - snapshot tables (read-optimized serving results)

CREATE TABLE IF NOT EXISTS board_stats (
    board_id BIGINT NOT NULL,
    total_views BIGINT NOT NULL DEFAULT 0,
    total_likes BIGINT NOT NULL DEFAULT 0,
    total_comments BIGINT NOT NULL DEFAULT 0,
    score DECIMAL(18,6) NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (board_id),
    INDEX idx_board_stats_score (score),
    INDEX idx_board_stats_updated_at (updated_at)
);

CREATE TABLE IF NOT EXISTS recipe_stats (
    recipe_id BIGINT NOT NULL,
    total_views BIGINT NOT NULL DEFAULT 0,
    total_likes BIGINT NOT NULL DEFAULT 0,
    total_bookmarks BIGINT NOT NULL DEFAULT 0,
    score DECIMAL(18,6) NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (recipe_id),
    INDEX idx_recipe_stats_score (score),
    INDEX idx_recipe_stats_updated_at (updated_at)
);

CREATE TABLE IF NOT EXISTS board_popular_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    snapshot_type VARCHAR(40) NOT NULL,
    section_key VARCHAR(40) NOT NULL DEFAULT '',
    rank_no INT NOT NULL,
    board_id BIGINT NOT NULL,
    score DECIMAL(18,6) NOT NULL DEFAULT 0,
    generated_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_board_popular_snapshot_slot (snapshot_type, section_key, rank_no, generated_at),
    INDEX idx_board_popular_snapshot_lookup (snapshot_type, section_key, expires_at, rank_no),
    INDEX idx_board_popular_snapshot_board (board_id)
);

CREATE TABLE IF NOT EXISTS recipe_reco_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slot_date DATE NOT NULL,
    slot_type VARCHAR(20) NOT NULL,
    rank_no INT NOT NULL,
    recipe_id BIGINT NOT NULL,
    score DECIMAL(18,6) NOT NULL DEFAULT 0,
    generated_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_recipe_reco_snapshot_slot (slot_date, slot_type, rank_no),
    INDEX idx_recipe_reco_snapshot_lookup (slot_date, slot_type, expires_at, rank_no),
    INDEX idx_recipe_reco_snapshot_recipe (recipe_id)
);

INSERT INTO board_stats (board_id, total_views, total_likes, total_comments, score, updated_at)
SELECT b.id,
       COALESCE(b.views, 0),
       COALESCE(b.likeCount, 0),
       COALESCE(b.commentCount, 0),
       0,
       NOW(6)
FROM board b
WHERE NOT EXISTS (
    SELECT 1
    FROM board_stats bs
    WHERE bs.board_id = b.id
);
