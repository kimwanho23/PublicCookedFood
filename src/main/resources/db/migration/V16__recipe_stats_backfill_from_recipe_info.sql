INSERT INTO recipe_stats (recipe_id, total_views, total_likes, total_bookmarks, score, updated_at)
SELECT DISTINCT r.recipe_ID,
       0,
       0,
       0,
       0,
       NOW(6)
FROM Recipe_INFO r
WHERE r.recipe_ID IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM recipe_stats rs
    WHERE rs.recipe_id = r.recipe_ID
);
