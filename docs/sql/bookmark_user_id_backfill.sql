-- Bookmark mapping migration: email -> user_id
-- Run this once after deploying code that adds bookmark.user_id.
-- MySQL

UPDATE bookmark b
JOIN `user` u ON u.email = b.email
SET b.user_id = u.id
WHERE b.user_id IS NULL
  AND b.email IS NOT NULL;

-- Optional hardening after verifying backfill result:
-- 1) Ensure no null user_id rows remain
-- SELECT COUNT(*) FROM bookmark WHERE user_id IS NULL;
--
-- 2) Add or keep uniqueness by (user_id, recipe_ID)
-- ALTER TABLE bookmark ADD CONSTRAINT uk_bookmark_user_recipe UNIQUE (user_id, recipe_ID);
--
-- 3) (Optional) drop legacy email column if no longer needed by DB policy
-- ALTER TABLE bookmark DROP COLUMN email;
