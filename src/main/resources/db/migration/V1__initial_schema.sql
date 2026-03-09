-- Reset Flyway baseline from the current final schema.
-- Legacy migrations were intentionally replaced so the project can restart from V1.

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `accounts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `authority` enum('GUEST','USER','ADMIN') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `loginMethod` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `phoneNumber` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `birthDate` date DEFAULT NULL,
  `gender` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `address` varchar(120) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `addressDetail` varchar(120) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `profileImageUrl` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `notificationEnabled` bit(1) NOT NULL DEFAULT b'1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKob8kqyqqgmefl0aco34akdtpe` (`email`),
  UNIQUE KEY `uk_member_name` (`name`),
  KEY `idx_member_phone_number` (`phoneNumber`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `account_activity_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `action` varchar(80) COLLATE utf8mb4_general_ci NOT NULL,
  `detail` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `regTime` datetime(6) DEFAULT NULL,
  `updateTime` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_account_activity_account_regtime` (`account_id`,`regTime`),
  KEY `idx_account_activity_action_regtime` (`action`,`regTime`),
  CONSTRAINT `fk_account_activity_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `account_block` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `blocker_id` bigint NOT NULL,
  `blocked_id` bigint NOT NULL,
  `regTime` datetime(6) DEFAULT NULL,
  `updateTime` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_block_blocker_blocked` (`blocker_id`,`blocked_id`),
  KEY `idx_account_block_blocker_regtime` (`blocker_id`,`regTime`),
  KEY `idx_account_block_blocked_regtime` (`blocked_id`,`regTime`),
  CONSTRAINT `fk_account_block_blocked` FOREIGN KEY (`blocked_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_account_block_blocker` FOREIGN KEY (`blocker_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `board` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `regTime` datetime(6) DEFAULT NULL,
  `updateTime` datetime(6) DEFAULT NULL,
  `createdBy` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `modifiedBy` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `commentCount` bigint DEFAULT NULL,
  `contents` longtext COLLATE utf8mb4_general_ci,
  `likeCount` bigint DEFAULT NULL,
  `state` varchar(1) COLLATE utf8mb4_general_ci NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `views` bigint DEFAULT NULL,
  `account_id` bigint NOT NULL,
  `section_id` bigint DEFAULT NULL,
  `is_hidden_by_report` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_board_state_reg_time` (`state`,`regTime`),
  KEY `idx_board_state_title` (`state`,`title`),
  KEY `idx_board_account_id` (`account_id`),
  KEY `idx_board_state_section_reg_time` (`state`,`section_id`,`regTime`),
  KEY `idx_board_state_like_count` (`state`,`likeCount`),
  KEY `FKofjdicxhplloyi17wtysmi5a9` (`section_id`),
  KEY `idx_board_state_hidden_reg_time` (`state`,`is_hidden_by_report`,`regTime`),
  KEY `idx_board_state_hidden_section_reg_time` (`state`,`is_hidden_by_report`,`section_id`,`regTime`),
  KEY `idx_board_state_hidden_like_count_reg_time` (`state`,`is_hidden_by_report`,`likeCount`,`regTime`),
  CONSTRAINT `fk_board_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`),
  CONSTRAINT `FKofjdicxhplloyi17wtysmi5a9` FOREIGN KEY (`section_id`) REFERENCES `board_section` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `board_featured_ranking` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ranking_type` varchar(40) COLLATE utf8mb4_general_ci NOT NULL,
  `section_key` varchar(40) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `rank_no` int NOT NULL,
  `board_id` bigint NOT NULL,
  `score` decimal(18,6) NOT NULL DEFAULT '0.000000',
  `generated_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_board_featured_ranking_slot` (`ranking_type`,`section_key`,`rank_no`,`generated_at`),
  KEY `idx_board_featured_ranking_lookup` (`ranking_type`,`section_key`,`expires_at`,`rank_no`),
  KEY `idx_board_featured_ranking_board` (`board_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `board_image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `board_id` bigint NOT NULL,
  `image_id` bigint NOT NULL,
  `regTime` datetime(6) DEFAULT NULL,
  `updateTime` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_board_image_board_id_image_id` (`board_id`,`image_id`),
  KEY `idx_board_image_board_id` (`board_id`),
  KEY `idx_board_image_image_id` (`image_id`),
  CONSTRAINT `fk_board_image_board` FOREIGN KEY (`board_id`) REFERENCES `board` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_board_image_image` FOREIGN KEY (`image_id`) REFERENCES `images` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `board_policy` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `featuredLikeThreshold` int NOT NULL,
  `thumbnail_display_mode` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'LEFT',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `board_report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `board_id` bigint NOT NULL,
  `reporter_id` bigint NOT NULL,
  `reason` varchar(30) COLLATE utf8mb4_general_ci NOT NULL,
  `details` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'OPEN',
  `processed_by` bigint DEFAULT NULL,
  `processed_note` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `processed_time` datetime(6) DEFAULT NULL,
  `priority_score` int NOT NULL DEFAULT '0',
  `is_suspicious` bit(1) NOT NULL DEFAULT b'0',
  `regTime` datetime(6) DEFAULT NULL,
  `updateTime` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_board_report_status_regtime` (`status`,`regTime`),
  KEY `idx_board_report_reporter_regtime` (`reporter_id`,`regTime`),
  KEY `idx_board_report_board_regtime` (`board_id`,`regTime`),
  KEY `idx_board_report_processed_by_time` (`processed_by`,`processed_time`),
  KEY `idx_board_report_status_priority_regtime` (`status`,`is_suspicious`,`priority_score`,`regTime`),
  KEY `idx_board_report_board_reporter_status_regtime` (`board_id`,`reporter_id`,`status`,`regTime`),
  CONSTRAINT `fk_board_report_board` FOREIGN KEY (`board_id`) REFERENCES `board` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_board_report_processed_by` FOREIGN KEY (`processed_by`) REFERENCES `accounts` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_board_report_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `board_scrap` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `board_id` bigint NOT NULL,
  `regTime` datetime(6) DEFAULT NULL,
  `updateTime` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_board_scrap_account_board` (`account_id`,`board_id`),
  KEY `idx_board_scrap_account_regtime` (`account_id`,`regTime`),
  KEY `idx_board_scrap_board_regtime` (`board_id`,`regTime`),
  CONSTRAINT `fk_board_scrap_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_board_scrap_board` FOREIGN KEY (`board_id`) REFERENCES `board` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `board_section` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `displayOrder` int NOT NULL,
  `sectionKey` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `sectionName` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKac9r5ywwmr3xvj59nqtu38mv8` (`sectionKey`),
  KEY `idx_board_section_display_order` (`displayOrder`),
  KEY `idx_board_section_active_display_order` (`active`,`displayOrder`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `board_stats` (
  `board_id` bigint NOT NULL,
  `total_views` bigint NOT NULL DEFAULT '0',
  `total_likes` bigint NOT NULL DEFAULT '0',
  `total_comments` bigint NOT NULL DEFAULT '0',
  `score` decimal(18,6) NOT NULL DEFAULT '0.000000',
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`board_id`),
  KEY `idx_board_stats_score` (`score`),
  KEY `idx_board_stats_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `bookmark` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recipe_ID` bigint NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `account_id` bigint NOT NULL,
  `folder_name` varchar(60) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `tag_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bookmark_account_recipe` (`account_id`,`recipe_ID`),
  UNIQUE KEY `UKkp8jnjy8t7vqe8phe3avvmn2d` (`email`,`recipe_ID`),
  KEY `FKkdh419qvxulj97omjdnxk8wq7` (`recipe_ID`),
  CONSTRAINT `fk_bookmark_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`),
  CONSTRAINT `fk_bookmark_email_account` FOREIGN KEY (`email`) REFERENCES `accounts` (`email`),
  CONSTRAINT `FKkdh419qvxulj97omjdnxk8wq7` FOREIGN KEY (`recipe_ID`) REFERENCES `Recipe_INFO` (`row_NUM`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `comments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `regTime` datetime(6) DEFAULT NULL,
  `updateTime` datetime(6) DEFAULT NULL,
  `createdBy` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `modifiedBy` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `contents` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `state` varchar(1) COLLATE utf8mb4_general_ci NOT NULL,
  `post_id` bigint NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `root_parent_id` bigint DEFAULT NULL,
  `depth` int NOT NULL DEFAULT '0',
  `comment_path` varchar(2048) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `account_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_comments_account_id` (`account_id`),
  KEY `idx_comments_post_state` (`post_id`,`state`),
  KEY `idx_comments_post_reg_time` (`post_id`,`regTime`),
  KEY `idx_comments_parent_id` (`parent_id`),
  KEY `idx_comments_post_parent_reg_time` (`post_id`,`parent_id`,`regTime`),
  KEY `idx_comments_post_root_reg_time` (`post_id`,`root_parent_id`,`regTime`),
  KEY `idx_comments_post_root_depth_reg_time` (`post_id`,`root_parent_id`,`depth`,`regTime`),
  KEY `idx_comments_post_root_comment_path` (`post_id`,`root_parent_id`,`comment_path`),
  CONSTRAINT `FK1yvn1vr3w48cihunxlrwt28f2` FOREIGN KEY (`parent_id`) REFERENCES `comments` (`id`),
  CONSTRAINT `fk_comments_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FKqnr1nghwvbv9ehguo5adeg0hy` FOREIGN KEY (`post_id`) REFERENCES `board` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `images` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `imgUrl` varchar(500) COLLATE utf8mb4_general_ci NOT NULL,
  `post_id` bigint DEFAULT NULL,
  `regTime` datetime(6) DEFAULT NULL,
  `updateTime` datetime(6) DEFAULT NULL,
  `createdBy` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `modifiedBy` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `originalFilename` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `savedFilename` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `contentType` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `fileSize` bigint DEFAULT NULL,
  `status` enum('ATTACHED','DELETED','TEMP') COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_images_post_id_status` (`post_id`,`status`),
  KEY `idx_images_url_status` (`imgUrl`,`status`),
  CONSTRAINT `FKb235b9pdmmamu4fcmjclrjep7` FOREIGN KEY (`post_id`) REFERENCES `board` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `likes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `account_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_likes_account_board` (`account_id`,`post_id`),
  KEY `FK6b0cfsvjv3fldh8tf885xnr9i` (`post_id`),
  CONSTRAINT `FK6b0cfsvjv3fldh8tf885xnr9i` FOREIGN KEY (`post_id`) REFERENCES `board` (`id`),
  CONSTRAINT `fk_likes_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `receiver_id` bigint NOT NULL,
  `actor_id` bigint NOT NULL,
  `board_id` bigint NOT NULL,
  `comment_id` bigint DEFAULT NULL,
  `type` varchar(30) COLLATE utf8mb4_general_ci NOT NULL,
  `content_preview` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `is_read` bit(1) NOT NULL DEFAULT b'0',
  `read_time` datetime(6) DEFAULT NULL,
  `regTime` datetime(6) DEFAULT NULL,
  `updateTime` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_notification_receiver_read_regtime` (`receiver_id`,`is_read`,`regTime`),
  KEY `idx_notification_receiver_regtime` (`receiver_id`,`regTime`),
  KEY `idx_notification_board_comment` (`board_id`,`comment_id`),
  KEY `fk_notification_actor` (`actor_id`),
  KEY `fk_notification_comment` (`comment_id`),
  CONSTRAINT `fk_notification_actor` FOREIGN KEY (`actor_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_notification_board` FOREIGN KEY (`board_id`) REFERENCES `board` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_notification_comment` FOREIGN KEY (`comment_id`) REFERENCES `comments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_notification_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `recipe_review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `recipe_row_num` bigint NOT NULL,
  `rating` int NOT NULL,
  `contents` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `regTime` datetime(6) DEFAULT NULL,
  `updateTime` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recipe_review_account_recipe` (`account_id`,`recipe_row_num`),
  KEY `idx_recipe_review_recipe_regtime` (`recipe_row_num`,`regTime`),
  KEY `idx_recipe_review_recipe_rating` (`recipe_row_num`,`rating`),
  KEY `idx_recipe_review_account_regtime` (`account_id`,`regTime`),
  CONSTRAINT `fk_recipe_review_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_recipe_review_recipe` FOREIGN KEY (`recipe_row_num`) REFERENCES `Recipe_INFO` (`row_NUM`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `recipe_slot_recommendation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `slot_date` date NOT NULL,
  `slot_type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `rank_no` int NOT NULL,
  `recipe_id` bigint NOT NULL,
  `score` decimal(18,6) NOT NULL DEFAULT '0.000000',
  `generated_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recipe_slot_recommendation_slot` (`slot_date`,`slot_type`,`rank_no`),
  KEY `idx_recipe_slot_recommendation_lookup` (`slot_date`,`slot_type`,`expires_at`,`rank_no`),
  KEY `idx_recipe_slot_recommendation_recipe` (`recipe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `recipe_stats` (
  `recipe_id` bigint NOT NULL,
  `total_views` bigint NOT NULL DEFAULT '0',
  `total_likes` bigint NOT NULL DEFAULT '0',
  `total_bookmarks` bigint NOT NULL DEFAULT '0',
  `score` decimal(18,6) NOT NULL DEFAULT '0.000000',
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`recipe_id`),
  KEY `idx_recipe_stats_score` (`score`),
  KEY `idx_recipe_stats_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `Recipe_INFO` (
  `recipe_ID` bigint DEFAULT NULL,
  `row_NUM` bigint NOT NULL,
  `calorie` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `cooking_TIME` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `irdnt_CODE` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `level_NM` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `nation_CODE` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `nation_NM` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `pc_NM` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `qnt` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `recipe_NM_KO` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `sumry` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `ty_CODE` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `ty_NM` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `img_URL` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`row_NUM`),
  KEY `idx_recipe_info_recipe_id` (`recipe_ID`),
  KEY `idx_recipe_info_ty_nm` (`ty_NM`),
  KEY `idx_recipe_info_nation_nm` (`nation_NM`),
  KEY `idx_recipe_info_irdnt_code` (`irdnt_CODE`),
  KEY `idx_recipe_info_recipe_nm_ko` (`recipe_NM_KO`),
  KEY `idx_recipe_info_ty_nm_row_num` (`ty_NM`,`row_NUM`),
  KEY `idx_recipe_info_nation_nm_row_num` (`nation_NM`,`row_NUM`),
  KEY `idx_recipe_info_irdnt_code_row_num` (`irdnt_CODE`,`row_NUM`),
  KEY `idx_recipe_info_recipe_nm_ko_row_num` (`recipe_NM_KO`,`row_NUM`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `Recipe_IRDNT` (
  `recipe_ID` bigint DEFAULT NULL,
  `row_NUM` bigint NOT NULL,
  `irdnt_CPCTY` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `irdnt_NM` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `irdnt_SN` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `irdnt_TY_CODE` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `irdnt_TY_NM` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`row_NUM`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `Recipe_CRSE` (
  `recipe_ID` bigint DEFAULT NULL,
  `row_NUM` bigint NOT NULL,
  `cooking_DC` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `cooking_NO` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `step_TIP` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `img_URL` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`row_NUM`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `user_recipe` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `regTime` datetime(6) DEFAULT NULL,
  `updateTime` datetime(6) DEFAULT NULL,
  `createdBy` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `modifiedBy` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `account_id` bigint NOT NULL,
  `title` varchar(200) COLLATE utf8mb4_general_ci NOT NULL,
  `summary` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `thumbnail_url` varchar(500) COLLATE utf8mb4_general_ci NOT NULL,
  `cooking_time` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `servings` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `difficulty` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `state` varchar(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_user_recipe_account_regtime` (`account_id`,`regTime`),
  KEY `idx_user_recipe_state_regtime` (`state`,`regTime`),
  KEY `idx_user_recipe_state_title` (`state`,`title`),
  KEY `idx_user_recipe_state_account_regtime` (`state`,`account_id`,`regTime`),
  CONSTRAINT `fk_user_recipe_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `user_recipe_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `regTime` datetime(6) DEFAULT NULL,
  `updateTime` datetime(6) DEFAULT NULL,
  `createdBy` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `modifiedBy` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `account_id` bigint NOT NULL,
  `recipe_id` bigint NOT NULL,
  `contents` varchar(1000) COLLATE utf8mb4_general_ci NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `root_parent_id` bigint DEFAULT NULL,
  `depth` int NOT NULL DEFAULT '0',
  `comment_path` varchar(2048) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `state` varchar(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_user_recipe_comment_account_id` (`account_id`),
  KEY `idx_user_recipe_comment_recipe_state` (`recipe_id`,`state`),
  KEY `idx_user_recipe_comment_recipe_regtime` (`recipe_id`,`regTime`),
  KEY `idx_user_recipe_comment_parent_id` (`parent_id`),
  KEY `idx_user_recipe_comment_recipe_parent_regtime` (`recipe_id`,`parent_id`,`regTime`),
  KEY `idx_user_recipe_comment_recipe_root_regtime` (`recipe_id`,`root_parent_id`,`regTime`),
  KEY `idx_user_recipe_comment_recipe_root_depth_regtime` (`recipe_id`,`root_parent_id`,`depth`,`regTime`),
  KEY `idx_user_recipe_comment_recipe_root_comment_path` (`recipe_id`,`root_parent_id`,`comment_path`),
  CONSTRAINT `fk_user_recipe_comment_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_recipe_comment_parent` FOREIGN KEY (`parent_id`) REFERENCES `user_recipe_comment` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_recipe_comment_recipe` FOREIGN KEY (`recipe_id`) REFERENCES `user_recipe` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `user_recipe_ingredient` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recipe_id` bigint NOT NULL,
  `ingredient_group` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `ingredient_name` varchar(120) COLLATE utf8mb4_general_ci NOT NULL,
  `amount_text` varchar(120) COLLATE utf8mb4_general_ci NOT NULL,
  `sort_order` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_recipe_ingredient_recipe_group_sort` (`recipe_id`,`ingredient_group`,`sort_order`),
  KEY `idx_user_recipe_ingredient_recipe_group_sort` (`recipe_id`,`ingredient_group`,`sort_order`),
  CONSTRAINT `fk_user_recipe_ingredient_recipe` FOREIGN KEY (`recipe_id`) REFERENCES `user_recipe` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `user_recipe_review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `regTime` datetime(6) DEFAULT NULL,
  `updateTime` datetime(6) DEFAULT NULL,
  `account_id` bigint NOT NULL,
  `recipe_id` bigint NOT NULL,
  `rating` int NOT NULL,
  `contents` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `image_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_recipe_review_account_recipe` (`account_id`,`recipe_id`),
  KEY `idx_user_recipe_review_recipe_regtime` (`recipe_id`,`regTime`),
  KEY `idx_user_recipe_review_recipe_rating` (`recipe_id`,`rating`),
  KEY `idx_user_recipe_review_account_regtime` (`account_id`,`regTime`),
  CONSTRAINT `fk_user_recipe_review_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_recipe_review_recipe` FOREIGN KEY (`recipe_id`) REFERENCES `user_recipe` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `user_recipe_step` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recipe_id` bigint NOT NULL,
  `step_no` int NOT NULL,
  `contents` varchar(1000) COLLATE utf8mb4_general_ci NOT NULL,
  `tip` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `image_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_recipe_step_recipe_step_no` (`recipe_id`,`step_no`),
  KEY `idx_user_recipe_step_recipe_step_no` (`recipe_id`,`step_no`),
  CONSTRAINT `fk_user_recipe_step_recipe` FOREIGN KEY (`recipe_id`) REFERENCES `user_recipe` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET FOREIGN_KEY_CHECKS = 1;
