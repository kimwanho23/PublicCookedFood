package kwh.PublicCookedFood.userrecipe.dto.response;

public record UserRecipeIngredientResponse(Long id,
                                           String ingredientGroup,
                                           String ingredientGroupLabel,
                                           String ingredientName,
                                           String amountText,
                                           Integer sortOrder) {
}
