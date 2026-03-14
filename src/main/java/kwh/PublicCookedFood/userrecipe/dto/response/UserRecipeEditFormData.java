package kwh.PublicCookedFood.userrecipe.dto.response;

import java.util.List;

public record UserRecipeEditFormData(String title,
                                     String summary,
                                     String thumbnailUrl,
                                     String cookingTime,
                                     String servings,
                                     String difficulty,
                                     List<IngredientRow> ingredients,
                                     List<StepRow> steps) {

    public UserRecipeEditFormData {
        ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
        steps = steps == null ? List.of() : List.copyOf(steps);
    }

    public record IngredientRow(String ingredientGroup,
                                String ingredientName,
                                String amountText,
                                Integer sortOrder) {
    }

    public record StepRow(Integer stepNo,
                          String contents,
                          String tip,
                          String imageUrl) {
    }
}
