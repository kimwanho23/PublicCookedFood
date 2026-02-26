package kwh.PublicCookedFood.food.dto.response;

import java.time.LocalDateTime;

public record RecipeReviewResponse(
        Long userId,
        String userName,
        Integer rating,
        String contents,
        LocalDateTime regTime
) {
}
