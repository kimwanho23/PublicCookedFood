CREATE TABLE user_recipe (
    id BIGINT NOT NULL AUTO_INCREMENT,
    regTime DATETIME(6) NULL,
    updateTime DATETIME(6) NULL,
    createdBy VARCHAR(255) NULL,
    modifiedBy VARCHAR(255) NULL,
    account_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(500) NULL,
    thumbnail_url VARCHAR(500) NOT NULL,
    cooking_time VARCHAR(50) NULL,
    servings VARCHAR(50) NULL,
    difficulty VARCHAR(20) NULL,
    state VARCHAR(1) NOT NULL DEFAULT '1',
    PRIMARY KEY (id),
    INDEX idx_user_recipe_account_regtime (account_id, regTime),
    INDEX idx_user_recipe_state_regtime (state, regTime),
    INDEX idx_user_recipe_state_title (state, title),
    INDEX idx_user_recipe_state_account_regtime (state, account_id, regTime),
    CONSTRAINT fk_user_recipe_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE
);

CREATE TABLE user_recipe_ingredient (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipe_id BIGINT NOT NULL,
    ingredient_group VARCHAR(20) NOT NULL,
    ingredient_name VARCHAR(120) NOT NULL,
    amount_text VARCHAR(120) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_recipe_ingredient_recipe_group_sort (recipe_id, ingredient_group, sort_order),
    INDEX idx_user_recipe_ingredient_recipe_group_sort (recipe_id, ingredient_group, sort_order),
    CONSTRAINT fk_user_recipe_ingredient_recipe FOREIGN KEY (recipe_id) REFERENCES user_recipe (id) ON DELETE CASCADE
);

CREATE TABLE user_recipe_step (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipe_id BIGINT NOT NULL,
    step_no INT NOT NULL,
    contents VARCHAR(1000) NOT NULL,
    tip VARCHAR(500) NULL,
    image_url VARCHAR(500) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_recipe_step_recipe_step_no (recipe_id, step_no),
    INDEX idx_user_recipe_step_recipe_step_no (recipe_id, step_no),
    CONSTRAINT fk_user_recipe_step_recipe FOREIGN KEY (recipe_id) REFERENCES user_recipe (id) ON DELETE CASCADE
);

CREATE TABLE user_recipe_comment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    regTime DATETIME(6) NULL,
    updateTime DATETIME(6) NULL,
    createdBy VARCHAR(255) NULL,
    modifiedBy VARCHAR(255) NULL,
    account_id BIGINT NOT NULL,
    recipe_id BIGINT NOT NULL,
    contents VARCHAR(1000) NOT NULL,
    parent_id BIGINT NULL,
    root_parent_id BIGINT NULL,
    depth INT NOT NULL DEFAULT 0,
    comment_path VARCHAR(2048) CHARACTER SET ascii COLLATE ascii_bin NULL,
    state VARCHAR(1) NOT NULL DEFAULT '1',
    PRIMARY KEY (id),
    INDEX idx_user_recipe_comment_account_id (account_id),
    INDEX idx_user_recipe_comment_recipe_state (recipe_id, state),
    INDEX idx_user_recipe_comment_recipe_regtime (recipe_id, regTime),
    INDEX idx_user_recipe_comment_parent_id (parent_id),
    INDEX idx_user_recipe_comment_recipe_parent_regtime (recipe_id, parent_id, regTime),
    INDEX idx_user_recipe_comment_recipe_root_regtime (recipe_id, root_parent_id, regTime),
    INDEX idx_user_recipe_comment_recipe_root_depth_regtime (recipe_id, root_parent_id, depth, regTime),
    INDEX idx_user_recipe_comment_recipe_root_comment_path (recipe_id, root_parent_id, comment_path),
    CONSTRAINT fk_user_recipe_comment_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_recipe_comment_recipe FOREIGN KEY (recipe_id) REFERENCES user_recipe (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_recipe_comment_parent FOREIGN KEY (parent_id) REFERENCES user_recipe_comment (id) ON DELETE CASCADE
);

CREATE TABLE user_recipe_review (
    id BIGINT NOT NULL AUTO_INCREMENT,
    regTime DATETIME(6) NULL,
    updateTime DATETIME(6) NULL,
    account_id BIGINT NOT NULL,
    recipe_id BIGINT NOT NULL,
    rating TINYINT NOT NULL,
    contents VARCHAR(500) NULL,
    image_url VARCHAR(500) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_recipe_review_account_recipe (account_id, recipe_id),
    INDEX idx_user_recipe_review_recipe_regtime (recipe_id, regTime),
    INDEX idx_user_recipe_review_recipe_rating (recipe_id, rating),
    INDEX idx_user_recipe_review_account_regtime (account_id, regTime),
    CONSTRAINT fk_user_recipe_review_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_recipe_review_recipe FOREIGN KEY (recipe_id) REFERENCES user_recipe (id) ON DELETE CASCADE
);
