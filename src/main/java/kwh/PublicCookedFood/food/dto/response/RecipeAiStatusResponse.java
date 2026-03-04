package kwh.PublicCookedFood.food.dto.response;

import java.time.LocalDateTime;

public record RecipeAiStatusResponse(
        LocalDateTime checkedAt,
        boolean recommendationReady,
        long recipeDocumentCount,
        Long sampleRecipeId,
        boolean openAiEnabled,
        String message
) {
}
