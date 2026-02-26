package kwh.PublicCookedFood.food.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record RecipeAiRecommendResponse(
        LocalDateTime generatedAt,
        String query,
        int totalCandidates,
        int returnedCount,
        List<RecipeAiRecommendItemResponse> recommendations
) {
}
