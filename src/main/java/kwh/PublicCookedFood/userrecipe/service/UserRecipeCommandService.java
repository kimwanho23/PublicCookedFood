package kwh.PublicCookedFood.userrecipe.service;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.storage.ImageLifecycleService;
import kwh.PublicCookedFood.storage.ImageUrls;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipe;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipeIngredient;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipeIngredientGroup;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipeStep;
import kwh.PublicCookedFood.userrecipe.error.UserRecipeErrorCode;
import kwh.PublicCookedFood.userrecipe.repository.UserRecipeRepository;
import kwh.PublicCookedFood.userrecipe.repository.UserRecipeReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserRecipeCommandService {

    private static final int MAX_STEP_COUNT = 15;

    private final UserRecipeRepository userRecipeRepository;
    private final UserRecipeReviewRepository userRecipeReviewRepository;
    private final AccountRepository accountRepository;
    private final ImageLifecycleService imageLifecycleService;

    @Transactional
    public Long createRecipe(Long accountId, UserRecipeWriteCommand command) {
        validateWriteCommand(command);
        if (accountId == null) {
            throw new IllegalArgumentException("사용자 정보가 올바르지 않습니다.");
        }
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

        UserRecipe recipe = UserRecipe.builder()
                .account(account)
                .title(command.title())
                .summary(command.summary())
                .thumbnailUrl(command.thumbnailUrl())
                .cookingTime(command.cookingTime())
                .servings(command.servings())
                .difficulty(command.difficulty())
                .ingredients(buildIngredients(command.ingredients()))
                .steps(buildSteps(command.steps()))
                .build();

        UserRecipe savedRecipe = userRecipeRepository.save(recipe);
        imageLifecycleService.attachImagesIfPresent(ImageUrls.of(collectImageUrls(savedRecipe)));
        return savedRecipe.getId();
    }

    @Transactional
    public void updateRecipe(Long recipeId, Long accountId, UserRecipeWriteCommand command) {
        validateWriteCommand(command);
        UserRecipe recipe = getOwnedActiveRecipeEntity(recipeId, accountId);
        Set<String> previousImageUrls = collectImageUrls(recipe);
        List<UserRecipeIngredient> nextIngredients = buildIngredients(command.ingredients());
        List<UserRecipeStep> nextSteps = buildSteps(command.steps());

        recipe.updateRecipe(
                command.title(),
                command.summary(),
                command.thumbnailUrl(),
                command.cookingTime(),
                command.servings(),
                command.difficulty()
        );

        // Flush orphan removals first so unique keys like (recipe_id, ingredient_group, sort_order)
        // are released before the replacement rows are inserted.
        recipe.replaceIngredients(List.of());
        recipe.replaceSteps(List.of());
        userRecipeRepository.flush();

        recipe.replaceIngredients(nextIngredients);
        recipe.replaceSteps(nextSteps);
        Set<String> currentImageUrls = collectImageUrls(recipe);
        imageLifecycleService.attachImagesIfPresent(ImageUrls.of(currentImageUrls));
        previousImageUrls.removeAll(currentImageUrls);
        imageLifecycleService.cleanupImagesByUrlIfUnlinked(ImageUrls.of(previousImageUrls));
    }

    @Transactional
    public void deleteRecipe(Long recipeId, Long accountId) {
        UserRecipe recipe = getOwnedActiveRecipeEntity(recipeId, accountId);
        Set<String> imageUrlsToCleanup = collectImageUrls(recipe);
        imageUrlsToCleanup.addAll(collectReviewImageUrls(recipe.getId()));
        recipe.markDeleted();
        userRecipeRepository.flush();
        imageLifecycleService.cleanupImagesByUrlIfUnlinked(ImageUrls.of(imageUrlsToCleanup));
    }

    private void validateWriteCommand(UserRecipeWriteCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("레시피 작성 요청이 비어 있습니다.");
        }
        if (command.ingredients().isEmpty()) {
            throw new IllegalArgumentException("재료는 최소 1개 이상 필요합니다.");
        }
        if (command.steps().isEmpty()) {
            throw new IllegalArgumentException("조리 단계는 최소 1개 이상 필요합니다.");
        }
        if (command.steps().size() > MAX_STEP_COUNT) {
            throw new IllegalArgumentException("조리 단계는 최대 15단계까지 작성할 수 있습니다.");
        }
    }

    private List<UserRecipeIngredient> buildIngredients(List<UserRecipeWriteCommand.IngredientItem> requests) {
        Map<UserRecipeIngredientGroup, Integer> sortOrderByGroup = new EnumMap<>(UserRecipeIngredientGroup.class);
        List<UserRecipeIngredient> ingredients = new ArrayList<>();
        for (UserRecipeWriteCommand.IngredientItem request : requests) {
            UserRecipeIngredientGroup group = UserRecipeIngredientGroup.from(request.ingredientGroup());
            int sortOrder = sortOrderByGroup.getOrDefault(group, 0);
            ingredients.add(UserRecipeIngredient.builder()
                    .ingredientGroup(group)
                    .ingredientName(request.ingredientName())
                    .amountText(request.amountText())
                    .sortOrder(sortOrder)
                    .build());
            sortOrderByGroup.put(group, sortOrder + 1);
        }
        return ingredients;
    }

    private List<UserRecipeStep> buildSteps(List<UserRecipeWriteCommand.StepItem> requests) {
        List<UserRecipeStep> steps = new ArrayList<>();
        for (int index = 0; index < requests.size(); index++) {
            UserRecipeWriteCommand.StepItem request = requests.get(index);
            steps.add(UserRecipeStep.builder()
                    .stepNo(index + 1)
                    .contents(request.contents())
                    .tip(request.tip())
                    .imageUrl(request.imageUrl())
                    .build());
        }
        return steps;
    }

    private UserRecipe getOwnedActiveRecipeEntity(Long recipeId, Long accountId) {
        if (accountId == null) {
            throw new AppException(UserRecipeErrorCode.USER_RECIPE_FORBIDDEN);
        }
        UserRecipe recipe = userRecipeRepository.findByIdWithAccountAndState(recipeId, SoftDeleteState.ACTIVE)
                .orElseThrow(() -> new AppException(UserRecipeErrorCode.USER_RECIPE_NOT_FOUND));
        if (recipe.getAccount() == null || !accountId.equals(recipe.getAccount().getId())) {
            throw new AppException(UserRecipeErrorCode.USER_RECIPE_FORBIDDEN);
        }
        return recipe;
    }

    private Set<String> collectImageUrls(UserRecipe recipe) {
        Set<String> imageUrls = new LinkedHashSet<>();
        if (recipe == null) {
            return imageUrls;
        }
        addImageUrlIfPresent(imageUrls, recipe.getThumbnailUrl());
        recipe.getSteps().forEach(step -> addImageUrlIfPresent(imageUrls, step.getImageUrl()));
        return imageUrls;
    }

    private Set<String> collectReviewImageUrls(Long recipeId) {
        Set<String> imageUrls = new LinkedHashSet<>();
        if (recipeId == null) {
            return imageUrls;
        }
        userRecipeReviewRepository.findImageUrlsByRecipeId(recipeId)
                .forEach(imageUrl -> addImageUrlIfPresent(imageUrls, imageUrl));
        return imageUrls;
    }

    private void addImageUrlIfPresent(Set<String> imageUrls, String imageUrl) {
        if (imageUrls == null || imageUrl == null) {
            return;
        }
        String trimmed = imageUrl.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        imageUrls.add(trimmed);
    }
}
