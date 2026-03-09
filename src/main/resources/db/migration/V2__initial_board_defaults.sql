-- Seed the minimum board defaults expected by the application on a fresh database.

INSERT INTO board_policy (id, featuredLikeThreshold, thumbnail_display_mode)
VALUES (1, 5, 'LEFT');

INSERT INTO board_section (id, active, displayOrder, sectionKey, sectionName)
VALUES
    (1, b'1', 1, 'general', '일반'),
    (2, b'1', 0, 'section', '레시피');
