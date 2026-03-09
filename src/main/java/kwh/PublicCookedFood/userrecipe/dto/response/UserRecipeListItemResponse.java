package kwh.PublicCookedFood.userrecipe.dto.response;

import java.time.LocalDateTime;

public record UserRecipeListItemResponse(Long id,
                                         Long accountId,
                                         String authorName,
                                         String title,
                                         String summary,
                                         String thumbnailUrl,
                                         String cookingTime,
                                         String servings,
                                         String difficulty,
                                         LocalDateTime regTime) {
}
