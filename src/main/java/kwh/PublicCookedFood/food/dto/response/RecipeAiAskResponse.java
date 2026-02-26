package kwh.PublicCookedFood.food.dto.response;

import java.time.LocalDateTime;

public record RecipeAiAskResponse(
        LocalDateTime generatedAt,
        Long recipeId,
        String recipeName,
        String question,
        String answer,
        boolean modelGenerated
) {
}
