package kwh.PublicCookedFood.food.facade;

import kwh.PublicCookedFood.food.dto.response.RecipeReviewResponse;
import kwh.PublicCookedFood.food.dto.response.RecipeReviewSummaryResponse;
import kwh.PublicCookedFood.food.dto.response.recipe_crse.Recipe_CRSE_ResponseDto;
import kwh.PublicCookedFood.food.dto.response.recipe_info.Recipe_INFO_ResponseDto;
import kwh.PublicCookedFood.food.dto.response.recipe_irdnt.Recipe_IRDNT_ResponseDto;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.metrics.reco.RecipeScoreEventPublisher;
import kwh.PublicCookedFood.food.service.RecipeReviewService;
import kwh.PublicCookedFood.food.service.RecipeService;
import kwh.PublicCookedFood.metrics.view.RecipeViewCounterService;
import kwh.PublicCookedFood.account.audit.RecipeAuditPublisher;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecipeDetailFacade {

    private static final List<String> RECIPE_INGREDIENT_CATEGORIES = List.of("\uC8FC\uC7AC\uB8CC", "\uBD80\uC7AC\uB8CC", "\uC591\uB150");

    private final RecipeService recipeService;
    private final BookmarkService bookmarkService;
    private final RecipeReviewService recipeReviewService;
    private final RecipeAuditPublisher recipeAuditPublisher;
    private final RecipeViewCounterService recipeViewCounterService;
    private final RecipeScoreEventPublisher recipeScoreEventPublisher;

    public RecipeDetailViewData loadRecipeDetail(Long recipeId,
                                                 Account account,
                                                 boolean increaseViews) {
        Recipe_INFO recipeInfo = recipeService.getRecipeEntityByRecipeId(recipeId);
        Recipe_INFO_ResponseDto infoResponseDto = recipeService.toInfoResponse(recipeInfo);
        List<Recipe_IRDNT_ResponseDto> irdntResponseDto = recipeService.getRecipeIrdnt(recipeId);
        List<Recipe_CRSE_ResponseDto> crseResponseDto = recipeService.getRecipeCrse(recipeId);
        long viewCount = increaseViews
                ? recipeViewCounterService.increaseRecipeViewAndGet(recipeId)
                : recipeViewCounterService.getRecipeViewCount(recipeId);

        boolean isLoggedIn = account != null;
        boolean isBookmarked = isLoggedIn && bookmarkService.isBookmarked(account, recipeInfo);
        RecipeReviewSummaryResponse reviewSummary = recipeReviewService.getSummary(recipeId);
        List<RecipeReviewResponse> reviews = recipeReviewService.getRecentReviews(recipeId);
        RecipeReviewResponse myReview = isLoggedIn ? recipeReviewService.getMyReview(recipeId, account.getId()) : null;

        return new RecipeDetailViewData(
                isBookmarked,
                RECIPE_INGREDIENT_CATEGORIES,
                infoResponseDto,
                irdntResponseDto,
                crseResponseDto,
                viewCount,
                reviewSummary,
                reviews,
                myReview
        );
    }

    @Transactional
    public ReviewUpsertResult upsertReview(Long recipeId,
                                           Long accountId,
                                           Integer rating,
                                           String contents) {
        try {
            recipeReviewService.upsertReview(recipeId, accountId, rating, contents);
            recipeScoreEventPublisher.publishRecalculateRequest(recipeId, "REVIEW_UPSERT");
            recipeAuditPublisher.recipeReviewUpsert(accountId, recipeId, rating);
            return ReviewUpsertResult.success("리뷰가 저장되었습니다.");
        } catch (IllegalArgumentException e) {
            recipeAuditPublisher.recipeReviewUpsertFailed(accountId, recipeId, e.getMessage());
            return ReviewUpsertResult.failure(e.getMessage());
        }
    }

    public record RecipeDetailViewData(boolean bookmarked,
                                       List<String> categories,
                                       Recipe_INFO_ResponseDto infoResponseDto,
                                       List<Recipe_IRDNT_ResponseDto> irdntResponseDto,
                                       List<Recipe_CRSE_ResponseDto> crseResponseDto,
                                       long viewCount,
                                       RecipeReviewSummaryResponse reviewSummary,
                                       List<RecipeReviewResponse> reviews,
                                       RecipeReviewResponse myReview) {
        public RecipeDetailViewData {
            categories = categories == null ? List.of() : List.copyOf(categories);
            irdntResponseDto = irdntResponseDto == null ? List.of() : List.copyOf(irdntResponseDto);
            crseResponseDto = crseResponseDto == null ? List.of() : List.copyOf(crseResponseDto);
            reviews = reviews == null ? List.of() : List.copyOf(reviews);
        }
    }

    public record ReviewUpsertResult(boolean success,
                                     String message) {

        public static ReviewUpsertResult success(String message) {
            return new ReviewUpsertResult(true, message);
        }

        public static ReviewUpsertResult failure(String message) {
            return new ReviewUpsertResult(false, message);
        }
    }
}
