package kwh.PublicCookedFood.metrics.view;

public interface RecipeViewCounterService {

    long increaseRecipeViewAndGet(Long recipeId);

    long getRecipeViewCount(Long recipeId);
}
