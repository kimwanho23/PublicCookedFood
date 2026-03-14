package kwh.PublicCookedFood.userrecipe.service;

import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipe;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipeIngredient;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipeStep;
import kwh.PublicCookedFood.userrecipe.dto.response.UserRecipeDetailResponse;
import kwh.PublicCookedFood.userrecipe.dto.response.UserRecipeEditFormData;
import kwh.PublicCookedFood.userrecipe.dto.response.UserRecipeIngredientResponse;
import kwh.PublicCookedFood.userrecipe.dto.response.UserRecipeListItemResponse;
import kwh.PublicCookedFood.userrecipe.dto.response.UserRecipeStepResponse;
import kwh.PublicCookedFood.userrecipe.error.UserRecipeErrorCode;
import kwh.PublicCookedFood.userrecipe.repository.UserRecipeIngredientRepository;
import kwh.PublicCookedFood.userrecipe.repository.UserRecipeRepository;
import kwh.PublicCookedFood.userrecipe.repository.UserRecipeStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserRecipeQueryService {

    private final UserRecipeRepository userRecipeRepository;
    private final UserRecipeIngredientRepository userRecipeIngredientRepository;
    private final UserRecipeStepRepository userRecipeStepRepository;

    @Transactional(readOnly = true)
    public Page<UserRecipeListItemResponse> getRecipeList(Pageable pageable) {
        return userRecipeRepository.findByStateOrderByRegTimeDesc(SoftDeleteState.ACTIVE, pageable)
                .map(recipe -> new UserRecipeListItemResponse(
                        recipe.getId(),
                        recipe.getAccount().getId(),
                        recipe.getAccount().getName(),
                        recipe.getTitle(),
                        recipe.getSummary(),
                        recipe.getThumbnailUrl(),
                        recipe.getCookingTime(),
                        recipe.getServings(),
                        recipe.getDifficulty(),
                        recipe.getRegTime()
                ));
    }

    @Transactional(readOnly = true)
    public UserRecipeDetailResponse getRecipeDetail(Long recipeId) {
        UserRecipe recipe = getActiveRecipeEntity(recipeId);
        List<UserRecipeIngredientResponse> ingredients = userRecipeIngredientRepository.findAllByRecipeId(recipeId).stream()
                .sorted(Comparator.comparing((UserRecipeIngredient ingredient) -> ingredient.getIngredientGroup().getDisplayOrder())
                        .thenComparing(UserRecipeIngredient::getSortOrder))
                .map(ingredient -> new UserRecipeIngredientResponse(
                        ingredient.getId(),
                        ingredient.getIngredientGroup().name(),
                        ingredient.getIngredientGroup().getDisplayName(),
                        ingredient.getIngredientName(),
                        ingredient.getAmountText(),
                        ingredient.getSortOrder()))
                .toList();
        List<UserRecipeStepResponse> steps = userRecipeStepRepository.findAllByRecipeIdOrderByStepNoAsc(recipeId).stream()
                .map(step -> new UserRecipeStepResponse(
                        step.getId(),
                        step.getStepNo(),
                        step.getContents(),
                        step.getTip(),
                        step.getImageUrl()))
                .toList();
        return new UserRecipeDetailResponse(
                recipe.getId(),
                recipe.getAccount().getId(),
                recipe.getAccount().getName(),
                recipe.getTitle(),
                recipe.getSummary(),
                recipe.getThumbnailUrl(),
                recipe.getCookingTime(),
                recipe.getServings(),
                recipe.getDifficulty(),
                ingredients,
                steps,
                recipe.getRegTime(),
                recipe.getUpdateTime()
        );
    }

    @Transactional(readOnly = true)
    public UserRecipeEditFormData getRecipeEditFormData(Long recipeId, Long accountId) {
        UserRecipe recipe = getOwnedActiveRecipeEntity(recipeId, accountId);
        return new UserRecipeEditFormData(
                recipe.getTitle(),
                recipe.getSummary(),
                recipe.getThumbnailUrl(),
                recipe.getCookingTime(),
                recipe.getServings(),
                recipe.getDifficulty(),
                recipe.getIngredients().stream()
                        .sorted(Comparator.comparing((UserRecipeIngredient ingredient) -> ingredient.getIngredientGroup().getDisplayOrder())
                                .thenComparing(UserRecipeIngredient::getSortOrder))
                        .map(ingredient -> new UserRecipeEditFormData.IngredientRow(
                                ingredient.getIngredientGroup().name(),
                                ingredient.getIngredientName(),
                                ingredient.getAmountText(),
                                ingredient.getSortOrder()
                        ))
                        .toList(),
                recipe.getSteps().stream()
                        .sorted(Comparator.comparing(UserRecipeStep::getStepNo))
                        .map(step -> new UserRecipeEditFormData.StepRow(
                                step.getStepNo(),
                                step.getContents(),
                                step.getTip(),
                                step.getImageUrl()
                        ))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public UserRecipe getActiveRecipeEntity(Long recipeId) {
        return userRecipeRepository.findByIdWithAccountAndState(recipeId, SoftDeleteState.ACTIVE)
                .orElseThrow(() -> new AppException(UserRecipeErrorCode.USER_RECIPE_NOT_FOUND));
    }

    private UserRecipe getOwnedActiveRecipeEntity(Long recipeId, Long accountId) {
        if (accountId == null) {
            throw new AppException(UserRecipeErrorCode.USER_RECIPE_FORBIDDEN);
        }
        UserRecipe recipe = getActiveRecipeEntity(recipeId);
        if (recipe.getAccount() == null || !accountId.equals(recipe.getAccount().getId())) {
            throw new AppException(UserRecipeErrorCode.USER_RECIPE_FORBIDDEN);
        }
        return recipe;
    }
}
