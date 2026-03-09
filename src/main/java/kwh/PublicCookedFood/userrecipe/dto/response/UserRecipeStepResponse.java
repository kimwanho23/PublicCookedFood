package kwh.PublicCookedFood.userrecipe.dto.response;

public record UserRecipeStepResponse(Long id,
                                     Integer stepNo,
                                     String contents,
                                     String tip,
                                     String imageUrl) {
}
