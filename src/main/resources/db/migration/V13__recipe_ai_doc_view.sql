-- Build AI-ready recipe document view from existing recipe tables.
-- - Uses recipe_info as canonical source so orphan step/ingredient rows are excluded.
-- - Normalizes numeric helper columns (minutes/servings/calorie) for filtering.
-- - Provides aggregated ingredient/step text and one ai_document field for LLM prompts.

SET @recipe_info_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'recipe_info'
    LIMIT 1
);

SET @recipe_irdnt_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'recipe_irdnt'
    LIMIT 1
);

SET @recipe_crse_table_name := (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'recipe_crse'
    LIMIT 1
);

SET @drop_recipe_ai_doc_view_sql := 'DROP VIEW IF EXISTS recipe_ai_doc';
PREPARE stmt_drop_recipe_ai_doc_view FROM @drop_recipe_ai_doc_view_sql;
EXECUTE stmt_drop_recipe_ai_doc_view;
DEALLOCATE PREPARE stmt_drop_recipe_ai_doc_view;

SET @create_recipe_ai_doc_view_sql := IF(
    @recipe_info_table_name IS NOT NULL
    AND @recipe_irdnt_table_name IS NOT NULL
    AND @recipe_crse_table_name IS NOT NULL,
    CONCAT(
        'CREATE VIEW recipe_ai_doc AS ',
        'SELECT ',
        'ri.row_NUM AS recipe_row_num, ',
        'ri.recipe_ID AS recipe_id, ',
        'TRIM(COALESCE(ri.recipe_NM_KO, '''')) AS recipe_name, ',
        'TRIM(COALESCE(ri.sumry, '''')) AS summary, ',
        'TRIM(COALESCE(ri.nation_NM, '''')) AS nation_name, ',
        'TRIM(COALESCE(ri.ty_NM, '''')) AS type_name, ',
        'TRIM(COALESCE(ri.level_NM, '''')) AS level_name, ',
        'TRIM(COALESCE(ri.cooking_TIME, '''')) AS cooking_time_text, ',
        'CAST(NULLIF(REGEXP_SUBSTR(TRIM(COALESCE(ri.cooking_TIME, '''')), ''[0-9]+'') , '''') AS UNSIGNED) AS cooking_time_minutes, ',
        'TRIM(COALESCE(ri.qnt, '''')) AS servings_text, ',
        'CAST(NULLIF(REGEXP_SUBSTR(TRIM(COALESCE(ri.qnt, '''')), ''[0-9]+'') , '''') AS UNSIGNED) AS servings_count, ',
        'TRIM(COALESCE(ri.calorie, '''')) AS calorie_text, ',
        'CAST(NULLIF(REGEXP_SUBSTR(TRIM(COALESCE(ri.calorie, '''')), ''[0-9]+'') , '''') AS UNSIGNED) AS calorie_kcal, ',
        'COALESCE(ing.ingredient_count, 0) AS ingredient_count, ',
        'COALESCE(step.step_count, 0) AS step_count, ',
        'COALESCE(ing.ingredient_lines, '''') AS ingredient_lines, ',
        'COALESCE(step.step_lines, '''') AS step_lines, ',
        'CONCAT(',
        '''이름: '', TRIM(COALESCE(ri.recipe_NM_KO, '''')), ',
        'CHAR(10), ''요약: '', TRIM(COALESCE(ri.sumry, '''')), ',
        'CHAR(10), ''분류: '', TRIM(COALESCE(ri.nation_NM, '''')), '' / '', TRIM(COALESCE(ri.ty_NM, '''')), ',
        'CHAR(10), ''난이도/시간/인분: '', TRIM(COALESCE(ri.level_NM, '''')), '' / '', TRIM(COALESCE(ri.cooking_TIME, '''')), '' / '', TRIM(COALESCE(ri.qnt, '''')), ',
        'CHAR(10), ''재료: '', COALESCE(ing.ingredient_lines, ''''), ',
        'CHAR(10), ''조리순서: '', COALESCE(step.step_lines, '''')',
        ') AS ai_document ',
        'FROM `', @recipe_info_table_name, '` ri ',
        'LEFT JOIN (',
            'SELECT ',
            'i.recipe_ID AS recipe_id, ',
            'COUNT(*) AS ingredient_count, ',
            'GROUP_CONCAT(',
                'CONCAT(',
                    'TRIM(COALESCE(i.irdnt_TY_NM, '''')), '': '', ',
                    'TRIM(COALESCE(i.irdnt_NM, '''')), ',
                    'CASE ',
                        'WHEN i.irdnt_CPCTY IS NULL OR TRIM(i.irdnt_CPCTY) = '''' THEN '''' ',
                        'ELSE CONCAT(''('', TRIM(i.irdnt_CPCTY), '')'') ',
                    'END',
                ') ',
                'ORDER BY CAST(i.irdnt_SN AS UNSIGNED), i.row_NUM ',
                'SEPARATOR '' | ''',
            ') AS ingredient_lines ',
            'FROM `', @recipe_irdnt_table_name, '` i ',
            'WHERE i.recipe_ID IS NOT NULL ',
            'GROUP BY i.recipe_ID',
        ') ing ON ing.recipe_id = ri.recipe_ID ',
        'LEFT JOIN (',
            'SELECT ',
            'c.recipe_ID AS recipe_id, ',
            'COUNT(*) AS step_count, ',
            'GROUP_CONCAT(',
                'CONCAT(',
                    'CAST(c.cooking_NO AS UNSIGNED), ''. '', ',
                    'TRIM(COALESCE(c.cooking_DC, '''')), ',
                    'CASE ',
                        'WHEN c.step_TIP IS NULL OR TRIM(c.step_TIP) = '''' THEN '''' ',
                        'ELSE CONCAT('' (팁: '', TRIM(c.step_TIP), '')'') ',
                    'END',
                ') ',
                'ORDER BY CAST(c.cooking_NO AS UNSIGNED), c.row_NUM ',
                'SEPARATOR '' | ''',
            ') AS step_lines ',
            'FROM `', @recipe_crse_table_name, '` c ',
            'WHERE c.recipe_ID IS NOT NULL ',
            'GROUP BY c.recipe_ID',
        ') step ON step.recipe_id = ri.recipe_ID ',
        'WHERE COALESCE(ing.ingredient_count, 0) > 0 ',
          'AND COALESCE(step.step_count, 0) > 0'
    ),
    'SELECT 1'
);

PREPARE stmt_create_recipe_ai_doc_view FROM @create_recipe_ai_doc_view_sql;
EXECUTE stmt_create_recipe_ai_doc_view;
DEALLOCATE PREPARE stmt_create_recipe_ai_doc_view;
