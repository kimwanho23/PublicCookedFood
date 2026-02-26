package kwh.PublicCookedFood.food.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RecipeAiDocRepository {

    private static final String SELECT_COLUMNS = """
            SELECT
                recipe_row_num,
                recipe_id,
                recipe_name,
                summary,
                nation_name,
                type_name,
                level_name,
                cooking_time_text,
                cooking_time_minutes,
                servings_text,
                servings_count,
                calorie_text,
                calorie_kcal,
                ingredient_count,
                step_count,
                ingredient_lines,
                step_lines,
                ai_document
            FROM recipe_ai_doc
            """;

    private static final String SELECT_ALL_SQL = SELECT_COLUMNS + " ORDER BY recipe_row_num";

    private static final String SELECT_BY_RECIPE_ID_SQL = SELECT_COLUMNS + " WHERE recipe_id = ? LIMIT 1";

    private static final RowMapper<RecipeAiDoc> RECIPE_AI_DOC_ROW_MAPPER = new RecipeAiDocRowMapper();

    private final JdbcTemplate jdbcTemplate;

    public List<RecipeAiDoc> findAll() {
        return jdbcTemplate.query(SELECT_ALL_SQL, RECIPE_AI_DOC_ROW_MAPPER);
    }

    public Optional<RecipeAiDoc> findByRecipeId(Long recipeId) {
        List<RecipeAiDoc> rows = jdbcTemplate.query(SELECT_BY_RECIPE_ID_SQL, RECIPE_AI_DOC_ROW_MAPPER, recipeId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    public record RecipeAiDoc(
            Long recipeRowNum,
            Long recipeId,
            String recipeName,
            String summary,
            String nationName,
            String typeName,
            String levelName,
            String cookingTimeText,
            Integer cookingTimeMinutes,
            String servingsText,
            Integer servingsCount,
            String calorieText,
            Integer calorieKcal,
            Integer ingredientCount,
            Integer stepCount,
            String ingredientLines,
            String stepLines,
            String aiDocument
    ) {
    }

    private static class RecipeAiDocRowMapper implements RowMapper<RecipeAiDoc> {
        @Override
        public RecipeAiDoc mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new RecipeAiDoc(
                    rs.getLong("recipe_row_num"),
                    rs.getLong("recipe_id"),
                    rs.getString("recipe_name"),
                    rs.getString("summary"),
                    rs.getString("nation_name"),
                    rs.getString("type_name"),
                    rs.getString("level_name"),
                    rs.getString("cooking_time_text"),
                    nullableInteger(rs, "cooking_time_minutes"),
                    rs.getString("servings_text"),
                    nullableInteger(rs, "servings_count"),
                    rs.getString("calorie_text"),
                    nullableInteger(rs, "calorie_kcal"),
                    nullableInteger(rs, "ingredient_count"),
                    nullableInteger(rs, "step_count"),
                    rs.getString("ingredient_lines"),
                    rs.getString("step_lines"),
                    rs.getString("ai_document")
            );
        }

        private Integer nullableInteger(ResultSet rs, String columnLabel) throws SQLException {
            Object value = rs.getObject(columnLabel);
            if (value == null) {
                return null;
            }
            if (value instanceof Number number) {
                return number.intValue();
            }
            return Integer.parseInt(value.toString());
        }
    }
}
