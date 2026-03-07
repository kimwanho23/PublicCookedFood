-- Remove empty legacy tables and align schema names with the current member-centric model.

SET @has_files := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'files'
);

SET @files_rows := 0;
SET @count_files_sql := IF(
    @has_files > 0,
    'SELECT COUNT(*) INTO @files_rows FROM files',
    'SELECT 0 INTO @files_rows'
);

PREPARE stmt_count_files FROM @count_files_sql;
EXECUTE stmt_count_files;
DEALLOCATE PREPARE stmt_count_files;

SET @drop_files_sql := IF(
    @has_files > 0 AND @files_rows = 0,
    'DROP TABLE files',
    'SELECT 1'
);

PREPARE stmt_drop_files FROM @drop_files_sql;
EXECUTE stmt_drop_files;
DEALLOCATE PREPARE stmt_drop_files;

SET @has_recipe_data := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'recipe_data'
);

SET @recipe_data_rows := 0;
SET @count_recipe_data_sql := IF(
    @has_recipe_data > 0,
    'SELECT COUNT(*) INTO @recipe_data_rows FROM recipe_data',
    'SELECT 0 INTO @recipe_data_rows'
);

PREPARE stmt_count_recipe_data FROM @count_recipe_data_sql;
EXECUTE stmt_count_recipe_data;
DEALLOCATE PREPARE stmt_count_recipe_data;

SET @drop_recipe_data_sql := IF(
    @has_recipe_data > 0 AND @recipe_data_rows = 0,
    'DROP TABLE recipe_data',
    'SELECT 1'
);

PREPARE stmt_drop_recipe_data FROM @drop_recipe_data_sql;
EXECUTE stmt_drop_recipe_data;
DEALLOCATE PREPARE stmt_drop_recipe_data;

RENAME TABLE
    user_block TO member_block,
    user_activity_log TO member_activity_log,
    board_popular_snapshot TO board_featured_ranking,
    recipe_reco_snapshot TO recipe_slot_recommendation;

ALTER TABLE board DROP FOREIGN KEY FKfyf1fchnby6hndhlfaidier1r;
ALTER TABLE board CHANGE COLUMN user_id member_id BIGINT NOT NULL;
ALTER TABLE board RENAME INDEX idx_board_user_id TO idx_board_member_id;
ALTER TABLE board
    ADD CONSTRAINT fk_board_member FOREIGN KEY (member_id) REFERENCES member (id);

ALTER TABLE board_scrap DROP FOREIGN KEY fk_board_scrap_user;
ALTER TABLE board_scrap CHANGE COLUMN user_id member_id BIGINT NOT NULL;
ALTER TABLE board_scrap RENAME INDEX uk_board_scrap_user_board TO uk_board_scrap_member_board;
ALTER TABLE board_scrap RENAME INDEX idx_board_scrap_user_regtime TO idx_board_scrap_member_regtime;
ALTER TABLE board_scrap
    ADD CONSTRAINT fk_board_scrap_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE;

ALTER TABLE bookmark DROP FOREIGN KEY FK3ogdxsxa4tx6vndyvpk1fk1am;
ALTER TABLE bookmark CHANGE COLUMN user_id member_id BIGINT NOT NULL;
ALTER TABLE bookmark RENAME INDEX UK5uegdxr9ddv4k7j9q2rx1sh0p TO uk_bookmark_member_recipe;
ALTER TABLE bookmark
    ADD CONSTRAINT fk_bookmark_member FOREIGN KEY (member_id) REFERENCES member (id);

ALTER TABLE comments DROP FOREIGN KEY FKhelkafkqj798li57pcpbvjug;
ALTER TABLE comments CHANGE COLUMN user_id member_id BIGINT NOT NULL;
ALTER TABLE comments RENAME INDEX FKhelkafkqj798li57pcpbvjug TO idx_comments_member_id;
ALTER TABLE comments
    ADD CONSTRAINT fk_comments_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE;

ALTER TABLE likes DROP FOREIGN KEY FKo8m7ie9t84ebyud050axtdcew;
ALTER TABLE likes CHANGE COLUMN user_id member_id BIGINT NOT NULL;
ALTER TABLE likes RENAME INDEX UKacbo1sjg3w5aveq7ola78mkxe TO uk_likes_member_board;
ALTER TABLE likes
    ADD CONSTRAINT fk_likes_member FOREIGN KEY (member_id) REFERENCES member (id);

ALTER TABLE recipe_review DROP FOREIGN KEY fk_recipe_review_user;
ALTER TABLE recipe_review CHANGE COLUMN user_id member_id BIGINT NOT NULL;
ALTER TABLE recipe_review RENAME INDEX uk_recipe_review_user_recipe TO uk_recipe_review_member_recipe;
ALTER TABLE recipe_review RENAME INDEX idx_recipe_review_user_regtime TO idx_recipe_review_member_regtime;
ALTER TABLE recipe_review
    ADD CONSTRAINT fk_recipe_review_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE;

ALTER TABLE member_activity_log DROP FOREIGN KEY fk_user_activity_user;
ALTER TABLE member_activity_log CHANGE COLUMN user_id member_id BIGINT NOT NULL;
ALTER TABLE member_activity_log RENAME INDEX idx_user_activity_user_regtime TO idx_member_activity_member_regtime;
ALTER TABLE member_activity_log RENAME INDEX idx_user_activity_action_regtime TO idx_member_activity_action_regtime;
ALTER TABLE member_activity_log
    ADD CONSTRAINT fk_member_activity_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE;

ALTER TABLE member_block DROP FOREIGN KEY fk_user_block_blocker;
ALTER TABLE member_block DROP FOREIGN KEY fk_user_block_blocked;
ALTER TABLE member_block RENAME INDEX uk_user_block_blocker_blocked TO uk_member_block_blocker_blocked;
ALTER TABLE member_block RENAME INDEX idx_user_block_blocker_regtime TO idx_member_block_blocker_regtime;
ALTER TABLE member_block RENAME INDEX idx_user_block_blocked_regtime TO idx_member_block_blocked_regtime;
ALTER TABLE member_block
    ADD CONSTRAINT fk_member_block_blocker FOREIGN KEY (blocker_id) REFERENCES member (id) ON DELETE CASCADE;
ALTER TABLE member_block
    ADD CONSTRAINT fk_member_block_blocked FOREIGN KEY (blocked_id) REFERENCES member (id) ON DELETE CASCADE;

ALTER TABLE board_featured_ranking CHANGE COLUMN snapshot_type ranking_type VARCHAR(40) NOT NULL;
ALTER TABLE board_featured_ranking RENAME INDEX uk_board_popular_snapshot_slot TO uk_board_featured_ranking_slot;
ALTER TABLE board_featured_ranking RENAME INDEX idx_board_popular_snapshot_lookup TO idx_board_featured_ranking_lookup;
ALTER TABLE board_featured_ranking RENAME INDEX idx_board_popular_snapshot_board TO idx_board_featured_ranking_board;

ALTER TABLE recipe_slot_recommendation RENAME INDEX uk_recipe_reco_snapshot_slot TO uk_recipe_slot_recommendation_slot;
ALTER TABLE recipe_slot_recommendation RENAME INDEX idx_recipe_reco_snapshot_lookup TO idx_recipe_slot_recommendation_lookup;
ALTER TABLE recipe_slot_recommendation RENAME INDEX idx_recipe_reco_snapshot_recipe TO idx_recipe_slot_recommendation_recipe;
