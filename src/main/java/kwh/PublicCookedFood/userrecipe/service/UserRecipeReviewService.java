package kwh.PublicCookedFood.userrecipe.service;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.storage.ImageLifecycleService;
import kwh.PublicCookedFood.storage.ImageUrls;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipe;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipeReview;
import kwh.PublicCookedFood.userrecipe.dto.response.UserRecipeReviewResponse;
import kwh.PublicCookedFood.userrecipe.dto.response.UserRecipeReviewSummaryResponse;
import kwh.PublicCookedFood.userrecipe.error.UserRecipeErrorCode;
import kwh.PublicCookedFood.userrecipe.repository.UserRecipeRepository;
import kwh.PublicCookedFood.userrecipe.repository.UserRecipeReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserRecipeReviewService {

    private static final int MAX_REVIEW_CONTENT_LENGTH = 500;
    private static final int MAX_REVIEW_IMAGE_URL_LENGTH = 500;

    private final UserRecipeReviewRepository userRecipeReviewRepository;
    private final UserRecipeRepository userRecipeRepository;
    private final AccountRepository accountRepository;
    private final AccountBlockService accountBlockService;
    private final ImageLifecycleService imageLifecycleService;

    @Transactional
    public void upsertReview(Long recipeId, Long accountId, Integer rating, String contents, String imageUrl) {
        if (recipeId == null || accountId == null) {
            throw new IllegalArgumentException("리뷰 요청 값이 올바르지 않습니다.");
        }
        validateRating(rating);

        UserRecipe recipe = userRecipeRepository.findByIdAndState(recipeId, SoftDeleteState.ACTIVE)
                .orElseThrow(() -> new AppException(UserRecipeErrorCode.USER_RECIPE_NOT_FOUND));
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));
        validateReviewAccess(recipe, accountId);
        String normalizedContents = normalizeText(contents, MAX_REVIEW_CONTENT_LENGTH, "리뷰 내용");
        String normalizedImageUrl = normalizeText(imageUrl, MAX_REVIEW_IMAGE_URL_LENGTH, "리뷰 이미지");
        UserRecipeReview existingReview = userRecipeReviewRepository.findByRecipeAndAccount(recipe, account).orElse(null);
        String previousImageUrl = existingReview == null ? null : existingReview.getImageUrl();

        java.util.Optional.ofNullable(existingReview)
                .ifPresentOrElse(
                        review -> review.update(rating, normalizedContents, normalizedImageUrl),
                        () -> userRecipeReviewRepository.save(UserRecipeReview.builder()
                                .recipe(recipe)
                                .account(account)
                                .rating(rating)
                                .contents(normalizedContents)
                                .imageUrl(normalizedImageUrl)
                                .build())
                );

        imageLifecycleService.attachImagesIfPresent(ImageUrls.single(normalizedImageUrl));
        if (previousImageUrl != null && !previousImageUrl.equals(normalizedImageUrl)) {
            imageLifecycleService.cleanupImagesByUrlIfUnlinked(ImageUrls.single(previousImageUrl));
        }
    }

    @Transactional
    public void deleteReview(Long recipeId, Long accountId) {
        if (recipeId == null || accountId == null) {
            throw new IllegalArgumentException("리뷰 요청 값이 올바르지 않습니다.");
        }
        UserRecipe recipe = userRecipeRepository.findByIdAndState(recipeId, SoftDeleteState.ACTIVE)
                .orElseThrow(() -> new AppException(UserRecipeErrorCode.USER_RECIPE_NOT_FOUND));
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));
        UserRecipeReview review = userRecipeReviewRepository.findByRecipeAndAccount(recipe, account)
                .orElseThrow(() -> new AppException(UserRecipeErrorCode.USER_RECIPE_REVIEW_NOT_FOUND));
        String imageUrl = review.getImageUrl();
        userRecipeReviewRepository.delete(review);
        userRecipeReviewRepository.flush();
        imageLifecycleService.cleanupImagesByUrlIfUnlinked(ImageUrls.single(imageUrl));
    }

    @Transactional(readOnly = true)
    public UserRecipeReviewSummaryResponse getSummary(Long recipeId) {
        UserRecipe recipe = userRecipeRepository.findByIdAndState(recipeId, SoftDeleteState.ACTIVE)
                .orElseThrow(() -> new AppException(UserRecipeErrorCode.USER_RECIPE_NOT_FOUND));
        long reviewCount = userRecipeReviewRepository.countByRecipe(recipe);
        double averageRating = userRecipeReviewRepository.findAverageRatingByRecipe(recipe);
        return new UserRecipeReviewSummaryResponse(round(averageRating), reviewCount);
    }

    @Transactional(readOnly = true)
    public List<UserRecipeReviewResponse> getRecentReviews(Long recipeId) {
        UserRecipe recipe = userRecipeRepository.findByIdAndState(recipeId, SoftDeleteState.ACTIVE)
                .orElseThrow(() -> new AppException(UserRecipeErrorCode.USER_RECIPE_NOT_FOUND));
        return userRecipeReviewRepository.findTop20ByRecipeOrderByRegTimeDesc(recipe).stream()
                .map(review -> new UserRecipeReviewResponse(
                        review.getAccount().getId(),
                        review.getAccount().getName(),
                        review.getRating(),
                        review.getContents(),
                        review.getImageUrl(),
                        review.getRegTime()))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserRecipeReviewResponse getMyReview(Long recipeId, Long accountId) {
        if (recipeId == null || accountId == null) {
            return null;
        }
        UserRecipe recipe = userRecipeRepository.findByIdAndState(recipeId, SoftDeleteState.ACTIVE).orElse(null);
        Account account = accountRepository.findById(accountId).orElse(null);
        if (recipe == null || account == null) {
            return null;
        }
        return userRecipeReviewRepository.findByRecipeAndAccount(recipe, account)
                .map(review -> new UserRecipeReviewResponse(
                        review.getAccount().getId(),
                        review.getAccount().getName(),
                        review.getRating(),
                        review.getContents(),
                        review.getImageUrl(),
                        review.getRegTime()))
                .orElse(null);
    }

    private double round(Double value) {
        if (value == null) {
            return 0.0d;
        }
        return Math.round(value * 10.0d) / 10.0d;
    }

    private String normalizeText(String rawText, int maxLength, String fieldLabel) {
        if (rawText == null) {
            return null;
        }
        String trimmed = rawText.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldLabel + "은 " + maxLength + "자 이하로 입력해주세요.");
        }
        return trimmed;
    }

    private void validateRating(Integer rating) {
        if (rating == null) {
            throw new IllegalArgumentException("리뷰 평점을 선택해주세요.");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("리뷰 평점은 1점부터 5점 사이여야 합니다.");
        }
    }

    private void validateReviewAccess(UserRecipe recipe, Long accountId) {
        if (recipe == null || accountId == null) {
            throw new IllegalArgumentException("리뷰 요청 값이 올바르지 않습니다.");
        }
        Long ownerId = recipe.getAccount() == null ? null : recipe.getAccount().getId();
        if (ownerId != null && ownerId.equals(accountId)) {
            throw new AppException(UserRecipeErrorCode.USER_RECIPE_REVIEW_SELF_FORBIDDEN);
        }
        if (ownerId != null && accountBlockService.isEitherBlocked(accountId, ownerId)) {
            throw new AppException(UserRecipeErrorCode.USER_RECIPE_REVIEW_BLOCKED);
        }
    }

}
