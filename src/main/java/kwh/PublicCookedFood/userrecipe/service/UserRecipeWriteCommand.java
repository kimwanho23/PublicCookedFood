package kwh.PublicCookedFood.userrecipe.service;

import java.util.List;

public record UserRecipeWriteCommand(String title,
                                     String summary,
                                     String thumbnailUrl,
                                     String cookingTime,
                                     String servings,
                                     String difficulty,
                                     List<IngredientItem> ingredients,
                                     List<StepItem> steps) {

    public UserRecipeWriteCommand {
        ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
        steps = steps == null ? List.of() : List.copyOf(steps);
    }

    public record IngredientItem(String ingredientGroup,
                                 String ingredientName,
                                 String amountText) {
    }

    public record StepItem(String contents,
                           String tip,
                           String imageUrl) {
    }
}
