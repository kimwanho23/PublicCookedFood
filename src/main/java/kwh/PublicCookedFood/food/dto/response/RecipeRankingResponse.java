package kwh.PublicCookedFood.food.dto.response;

public record RecipeRankingResponse(
        Long recipeId,
        String recipeName,
        Long reviewCount,
        double averageRating
) {
}
