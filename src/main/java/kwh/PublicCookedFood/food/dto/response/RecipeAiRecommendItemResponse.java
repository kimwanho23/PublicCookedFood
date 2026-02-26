package kwh.PublicCookedFood.food.dto.response;

public record RecipeAiRecommendItemResponse(
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
        int score,
        String reason,
        String recipeUrl
) {
}
