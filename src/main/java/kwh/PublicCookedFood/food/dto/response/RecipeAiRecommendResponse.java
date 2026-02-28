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
    public RecipeAiRecommendResponse {
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    }

    @Override
    public List<RecipeAiRecommendItemResponse> recommendations() {
        return List.copyOf(recommendations);
    }
}
