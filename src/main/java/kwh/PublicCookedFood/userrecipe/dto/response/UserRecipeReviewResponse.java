package kwh.PublicCookedFood.userrecipe.dto.response;

import java.time.LocalDateTime;

public record UserRecipeReviewResponse(Long accountId,
                                       String accountName,
                                       Integer rating,
                                       String contents,
                                       String imageUrl,
                                       LocalDateTime regTime) {
}
