package kwh.PublicCookedFood.food.dto.response;

import java.time.LocalDateTime;

public record RecipeReviewResponse(
        Long accountId,
        String accountName,
        Integer rating,
        String contents,
        LocalDateTime regTime
) {
}
