package kwh.PublicCookedFood.userrecipe.service;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.service.ImageService;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipe;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipeIngredient;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipeIngredientGroup;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipeStep;
import kwh.PublicCookedFood.userrecipe.dto.request.UserRecipeIngredientRequest;
import kwh.PublicCookedFood.userrecipe.dto.request.UserRecipeStepRequest;
import kwh.PublicCookedFood.userrecipe.dto.request.UserRecipeWriteRequest;
import kwh.PublicCookedFood.userrecipe.dto.response.UserRecipeDetailResponse;
import kwh.PublicCookedFood.userrecipe.dto.response.UserRecipeIngredientResponse;
import kwh.PublicCookedFood.userrecipe.dto.response.UserRecipeListItemResponse;
import kwh.PublicCookedFood.userrecipe.dto.response.UserRecipeStepResponse;
import kwh.PublicCookedFood.userrecipe.error.UserRecipeErrorCode;
import kwh.PublicCookedFood.userrecipe.repository.UserRecipeIngredientRepository;
import kwh.PublicCookedFood.userrecipe.repository.UserRecipeRepository;
import kwh.PublicCookedFood.userrecipe.repository.UserRecipeReviewRepository;
import kwh.PublicCookedFood.userrecipe.repository.UserRecipeStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserRecipeService {

    private static final int MAX_STEP_COUNT = 15;

    private final UserRecipeRepository userRecipeRepository;
    private final UserRecipeIngredientRepository userRecipeIngredientRepository;
    private final UserRecipeStepRepository userRecipeStepRepository;
    private final UserRecipeReviewRepository userRecipeReviewRepository;
    private final AccountRepository accountRepository;
    private final ImageService imageService;

    @Transactional
    public Long createRecipe(Long accountId, UserRecipeWriteRequest request) {
        validateWriteRequest(request);
        if (accountId == null) {
            throw new IllegalArgumentException("사용자 정보가 올바르지 않습니다.");
        }
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

        UserRecipe recipe = UserRecipe.builder()
                .account(account)
                .title(request.getTitle())
                .summary(request.getSummary())
                .thumbnailUrl(request.getThumbnailUrl())
                .cookingTime(request.getCookingTime())
                .servings(request.getServings())
                .difficulty(request.getDifficulty())
                .ingredients(buildIngredients(request.getIngredients()))
                .steps(buildSteps(request.getSteps()))
                .build();

        UserRecipe savedRecipe = userRecipeRepository.save(recipe);
        imageService.attachImagesIfPresent(collectImageUrls(savedRecipe));
        return savedRecipe.getId();
    }

    @Transactional
    public void updateRecipe(Long recipeId, Long accountId, UserRecipeWriteRequest request) {
        validateWriteRequest(request);
        UserRecipe recipe = getOwnedActiveRecipeEntity(recipeId, accountId);
        Set<String> previousImageUrls = collectImageUrls(recipe);
        List<UserRecipeIngredient> nextIngredients = buildIngredients(request.getIngredients());
        List<UserRecipeStep> nextSteps = buildSteps(request.getSteps());

        recipe.updateRecipe(
                request.getTitle(),
                request.getSummary(),
                request.getThumbnailUrl(),
                request.getCookingTime(),
                request.getServings(),
                request.getDifficulty()
        );

        // Flush orphan removals first so unique keys like (recipe_id, ingredient_group, sort_order)
        // are released before the replacement rows are inserted.
        recipe.replaceIngredients(List.of());
        recipe.replaceSteps(List.of());
        userRecipeRepository.flush();

        recipe.replaceIngredients(nextIngredients);
        recipe.replaceSteps(nextSteps);
        Set<String> currentImageUrls = collectImageUrls(recipe);
        imageService.attachImagesIfPresent(currentImageUrls);
        previousImageUrls.removeAll(currentImageUrls);
        imageService.cleanupImagesByUrlIfUnlinked(previousImageUrls);
    }

    @Transactional
    public void deleteRecipe(Long recipeId, Long accountId) {
        UserRecipe recipe = getOwnedActiveRecipeEntity(recipeId, accountId);
        Set<String> imageUrlsToCleanup = collectImageUrls(recipe);
        imageUrlsToCleanup.addAll(collectReviewImageUrls(recipe.getId()));
        recipe.markDeleted();
        userRecipeRepository.flush();
        imageService.cleanupImagesByUrlIfUnlinked(imageUrlsToCleanup);
    }

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
    public UserRecipeWriteRequest getRecipeWriteRequest(Long recipeId, Long accountId) {
        UserRecipe recipe = getOwnedActiveRecipeEntity(recipeId, accountId);
        UserRecipeWriteRequest request = new UserRecipeWriteRequest();
        request.setTitle(recipe.getTitle());
        request.setSummary(recipe.getSummary());
        request.setThumbnailUrl(recipe.getThumbnailUrl());
        request.setCookingTime(recipe.getCookingTime());
        request.setServings(recipe.getServings());
        request.setDifficulty(recipe.getDifficulty());
        request.setIngredients(recipe.getIngredients().stream()
                .sorted(Comparator.comparing((UserRecipeIngredient ingredient) -> ingredient.getIngredientGroup().getDisplayOrder())
                        .thenComparing(UserRecipeIngredient::getSortOrder))
                .map(ingredient -> {
                    UserRecipeIngredientRequest ingredientRequest = new UserRecipeIngredientRequest();
                    ingredientRequest.setIngredientGroup(ingredient.getIngredientGroup().name());
                    ingredientRequest.setIngredientName(ingredient.getIngredientName());
                    ingredientRequest.setAmountText(ingredient.getAmountText());
                    ingredientRequest.setSortOrder(ingredient.getSortOrder());
                    return ingredientRequest;
                })
                .toList());
        request.setSteps(recipe.getSteps().stream()
                .sorted(Comparator.comparing(UserRecipeStep::getStepNo))
                .map(step -> {
                    UserRecipeStepRequest stepRequest = new UserRecipeStepRequest();
                    stepRequest.setStepNo(step.getStepNo());
                    stepRequest.setContents(step.getContents());
                    stepRequest.setTip(step.getTip());
                    stepRequest.setImageUrl(step.getImageUrl());
                    return stepRequest;
                })
                .toList());
        return request;
    }

    @Transactional(readOnly = true)
    public UserRecipe getActiveRecipeEntity(Long recipeId) {
        return userRecipeRepository.findByIdWithAccountAndState(recipeId, SoftDeleteState.ACTIVE)
                .orElseThrow(() -> new AppException(UserRecipeErrorCode.USER_RECIPE_NOT_FOUND));
    }

    private void validateWriteRequest(UserRecipeWriteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("레시피 작성 요청이 비어 있습니다.");
        }
        if (request.getIngredients() == null || request.getIngredients().isEmpty()) {
            throw new IllegalArgumentException("재료는 최소 1개 이상 필요합니다.");
        }
        if (request.getSteps() == null || request.getSteps().isEmpty()) {
            throw new IllegalArgumentException("조리 단계는 최소 1개 이상 필요합니다.");
        }
        if (request.getSteps().size() > MAX_STEP_COUNT) {
            throw new IllegalArgumentException("조리 단계는 최대 15단계까지 작성할 수 있습니다.");
        }
    }

    private List<UserRecipeIngredient> buildIngredients(List<UserRecipeIngredientRequest> requests) {
        Map<UserRecipeIngredientGroup, Integer> sortOrderByGroup = new EnumMap<>(UserRecipeIngredientGroup.class);
        List<UserRecipeIngredient> ingredients = new ArrayList<>();
        for (UserRecipeIngredientRequest request : requests) {
            UserRecipeIngredientGroup group = UserRecipeIngredientGroup.from(request.getIngredientGroup());
            int sortOrder = sortOrderByGroup.getOrDefault(group, 0);
            ingredients.add(UserRecipeIngredient.builder()
                    .ingredientGroup(group)
                    .ingredientName(request.getIngredientName())
                    .amountText(request.getAmountText())
                    .sortOrder(sortOrder)
                    .build());
            sortOrderByGroup.put(group, sortOrder + 1);
        }
        return ingredients;
    }

    private List<UserRecipeStep> buildSteps(List<UserRecipeStepRequest> requests) {
        List<UserRecipeStep> steps = new ArrayList<>();
        for (int index = 0; index < requests.size(); index++) {
            UserRecipeStepRequest request = requests.get(index);
            steps.add(UserRecipeStep.builder()
                    .stepNo(index + 1)
                    .contents(request.getContents())
                    .tip(request.getTip())
                    .imageUrl(request.getImageUrl())
                    .build());
        }
        return steps;
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
