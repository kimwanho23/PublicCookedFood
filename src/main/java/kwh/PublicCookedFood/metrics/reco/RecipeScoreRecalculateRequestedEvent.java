package kwh.PublicCookedFood.metrics.reco;

public record RecipeScoreRecalculateRequestedEvent(Long recipeId,
                                                   String reason) {
}
