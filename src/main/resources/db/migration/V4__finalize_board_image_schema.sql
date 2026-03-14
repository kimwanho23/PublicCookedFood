ALTER TABLE `images`
    MODIFY COLUMN `post_id` BIGINT NULL;

CREATE TABLE IF NOT EXISTS `board_image` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `board_id` BIGINT NOT NULL,
    `image_id` BIGINT NOT NULL,
    `regTime` DATETIME(6) NULL,
    `updateTime` DATETIME(6) NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_board_image_board_id_image_id` UNIQUE (`board_id`, `image_id`),
    INDEX `idx_board_image_board_id` (`board_id`),
    INDEX `idx_board_image_image_id` (`image_id`),
    CONSTRAINT `fk_board_image_board` FOREIGN KEY (`board_id`) REFERENCES `board` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_board_image_image` FOREIGN KEY (`image_id`) REFERENCES `images` (`id`) ON DELETE CASCADE
);

INSERT INTO `board_image` (`board_id`, `image_id`, `regTime`, `updateTime`)
SELECT i.`post_id`,
       i.`id`,
       COALESCE(i.`regTime`, CURRENT_TIMESTAMP(6)),
       COALESCE(i.`updateTime`, CURRENT_TIMESTAMP(6))
FROM `images` i
JOIN `board` b ON b.`id` = i.`post_id`
LEFT JOIN `board_image` bi ON bi.`board_id` = i.`post_id` AND bi.`image_id` = i.`id`
WHERE i.`post_id` IS NOT NULL
  AND bi.`id` IS NULL;

UPDATE `images` i
JOIN `board_image` bi ON bi.`image_id` = i.`id`
SET i.`status` = 'ATTACHED'
WHERE i.`status` IS NULL
   OR i.`status` <> 'DELETED';
