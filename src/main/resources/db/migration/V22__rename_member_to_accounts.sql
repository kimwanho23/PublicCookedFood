-- Promote member/account naming to a final accounts-based schema.

ALTER TABLE board DROP FOREIGN KEY fk_board_member;
ALTER TABLE board_scrap DROP FOREIGN KEY fk_board_scrap_member;
ALTER TABLE bookmark DROP FOREIGN KEY fk_bookmark_member;
ALTER TABLE bookmark DROP FOREIGN KEY FK72ux9h903oo291lnwdpxj7r1e;
ALTER TABLE comments DROP FOREIGN KEY fk_comments_member;
ALTER TABLE likes DROP FOREIGN KEY fk_likes_member;
ALTER TABLE recipe_review DROP FOREIGN KEY fk_recipe_review_member;
ALTER TABLE member_activity_log DROP FOREIGN KEY fk_member_activity_member;
ALTER TABLE member_block DROP FOREIGN KEY fk_member_block_blocker;
ALTER TABLE member_block DROP FOREIGN KEY fk_member_block_blocked;
ALTER TABLE board_report DROP FOREIGN KEY fk_board_report_processed_by;
ALTER TABLE board_report DROP FOREIGN KEY fk_board_report_reporter;
ALTER TABLE notification DROP FOREIGN KEY fk_notification_actor;
ALTER TABLE notification DROP FOREIGN KEY fk_notification_receiver;

RENAME TABLE
    member TO accounts,
    member_seq TO accounts_seq,
    member_block TO account_block,
    member_activity_log TO account_activity_log;

ALTER TABLE board CHANGE COLUMN member_id account_id BIGINT NOT NULL;
ALTER TABLE board RENAME INDEX idx_board_member_id TO idx_board_account_id;
ALTER TABLE board
    ADD CONSTRAINT fk_board_account FOREIGN KEY (account_id) REFERENCES accounts (id);

ALTER TABLE board_scrap CHANGE COLUMN member_id account_id BIGINT NOT NULL;
ALTER TABLE board_scrap RENAME INDEX uk_board_scrap_member_board TO uk_board_scrap_account_board;
ALTER TABLE board_scrap RENAME INDEX idx_board_scrap_member_regtime TO idx_board_scrap_account_regtime;
ALTER TABLE board_scrap
    ADD CONSTRAINT fk_board_scrap_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE;

ALTER TABLE bookmark CHANGE COLUMN member_id account_id BIGINT NOT NULL;
ALTER TABLE bookmark RENAME INDEX uk_bookmark_member_recipe TO uk_bookmark_account_recipe;
ALTER TABLE bookmark
    ADD CONSTRAINT fk_bookmark_account FOREIGN KEY (account_id) REFERENCES accounts (id);
ALTER TABLE bookmark
    ADD CONSTRAINT fk_bookmark_email_account FOREIGN KEY (email) REFERENCES accounts (email);

ALTER TABLE comments CHANGE COLUMN member_id account_id BIGINT NOT NULL;
ALTER TABLE comments RENAME INDEX idx_comments_member_id TO idx_comments_account_id;
ALTER TABLE comments
    ADD CONSTRAINT fk_comments_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE;

ALTER TABLE likes CHANGE COLUMN member_id account_id BIGINT NOT NULL;
ALTER TABLE likes RENAME INDEX uk_likes_member_board TO uk_likes_account_board;
ALTER TABLE likes
    ADD CONSTRAINT fk_likes_account FOREIGN KEY (account_id) REFERENCES accounts (id);

ALTER TABLE recipe_review CHANGE COLUMN member_id account_id BIGINT NOT NULL;
ALTER TABLE recipe_review RENAME INDEX uk_recipe_review_member_recipe TO uk_recipe_review_account_recipe;
ALTER TABLE recipe_review RENAME INDEX idx_recipe_review_member_regtime TO idx_recipe_review_account_regtime;
ALTER TABLE recipe_review
    ADD CONSTRAINT fk_recipe_review_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE;

ALTER TABLE account_activity_log CHANGE COLUMN member_id account_id BIGINT NOT NULL;
ALTER TABLE account_activity_log RENAME INDEX idx_member_activity_member_regtime TO idx_account_activity_account_regtime;
ALTER TABLE account_activity_log RENAME INDEX idx_member_activity_action_regtime TO idx_account_activity_action_regtime;
ALTER TABLE account_activity_log
    ADD CONSTRAINT fk_account_activity_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE;

ALTER TABLE account_block RENAME INDEX uk_member_block_blocker_blocked TO uk_account_block_blocker_blocked;
ALTER TABLE account_block RENAME INDEX idx_member_block_blocker_regtime TO idx_account_block_blocker_regtime;
ALTER TABLE account_block RENAME INDEX idx_member_block_blocked_regtime TO idx_account_block_blocked_regtime;
ALTER TABLE account_block
    ADD CONSTRAINT fk_account_block_blocker FOREIGN KEY (blocker_id) REFERENCES accounts (id) ON DELETE CASCADE;
ALTER TABLE account_block
    ADD CONSTRAINT fk_account_block_blocked FOREIGN KEY (blocked_id) REFERENCES accounts (id) ON DELETE CASCADE;

ALTER TABLE board_report
    ADD CONSTRAINT fk_board_report_processed_by FOREIGN KEY (processed_by) REFERENCES accounts (id) ON DELETE SET NULL;
ALTER TABLE board_report
    ADD CONSTRAINT fk_board_report_reporter FOREIGN KEY (reporter_id) REFERENCES accounts (id) ON DELETE CASCADE;
ALTER TABLE notification
    ADD CONSTRAINT fk_notification_actor FOREIGN KEY (actor_id) REFERENCES accounts (id) ON DELETE CASCADE;
ALTER TABLE notification
    ADD CONSTRAINT fk_notification_receiver FOREIGN KEY (receiver_id) REFERENCES accounts (id) ON DELETE CASCADE;
