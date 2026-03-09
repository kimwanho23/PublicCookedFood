package kwh.PublicCookedFood.userrecipe.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record UserRecipeDetailResponse(Long id,
                                       Long accountId,
                                       String authorName,
                                       String title,
                                       String summary,
                                       String thumbnailUrl,
                                       String cookingTime,
                                       String servings,
                                       String difficulty,
                                       List<UserRecipeIngredientResponse> ingredients,
                                       List<UserRecipeStepResponse> steps,
                                       LocalDateTime regTime,
                                       LocalDateTime updateTime) {

    public UserRecipeDetailResponse {
        ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
