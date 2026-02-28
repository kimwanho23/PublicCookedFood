package kwh.PublicCookedFood.food.facade;

import kwh.PublicCookedFood.food.dto.response.RecipeReviewResponse;
import kwh.PublicCookedFood.food.dto.response.RecipeReviewSummaryResponse;
import kwh.PublicCookedFood.food.dto.response.recipe_crse.Recipe_CRSE_ResponseDto;
import kwh.PublicCookedFood.food.dto.response.recipe_info.Recipe_INFO_ResponseDto;
import kwh.PublicCookedFood.food.dto.response.recipe_irdnt.Recipe_IRDNT_ResponseDto;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.food.service.RecipeReviewService;
import kwh.PublicCookedFood.food.service.RecipeService;
import kwh.PublicCookedFood.user.audit.RecipeAuditPublisher;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecipeDetailFacade {

    private static final List<String> RECIPE_INGREDIENT_CATEGORIES = List.of("주재료", "부재료", "양념");

    private final RecipeService recipeService;
    private final BookmarkService bookmarkService;
    private final RecipeReviewService recipeReviewService;
    private final RecipeAuditPublisher recipeAuditPublisher;

    public RecipeDetailViewData loadRecipeDetail(Long recipeId, Users user) {
        Recipe_INFO recipeInfo = recipeService.getRecipeEntityByRecipeId(recipeId);
        Recipe_INFO_ResponseDto infoResponseDto = recipeService.toInfoResponse(recipeInfo);
        List<Recipe_IRDNT_ResponseDto> irdntResponseDto = recipeService.getRecipeIrdnt(recipeId);
        List<Recipe_CRSE_ResponseDto> crseResponseDto = recipeService.getRecipeCrse(recipeId);

        boolean isLoggedIn = user != null;
        boolean isBookmarked = isLoggedIn && bookmarkService.isBookmarked(user, recipeInfo);
        RecipeReviewSummaryResponse reviewSummary = recipeReviewService.getSummary(recipeId);
        List<RecipeReviewResponse> reviews = recipeReviewService.getRecentReviews(recipeId);
        RecipeReviewResponse myReview = isLoggedIn ? recipeReviewService.getMyReview(recipeId, user.getId()) : null;

        return new RecipeDetailViewData(
                isBookmarked,
                RECIPE_INGREDIENT_CATEGORIES,
                infoResponseDto,
                irdntResponseDto,
                crseResponseDto,
                reviewSummary,
                reviews,
                myReview
        );
    }

    @Transactional
    public ReviewUpsertResult upsertReview(Long recipeId,
                                           Long userId,
                                           Integer rating,
                                           String contents) {
        try {
            recipeReviewService.upsertReview(recipeId, userId, rating, contents);
            recipeAuditPublisher.recipeReviewUpsert(userId, recipeId, rating);
            return ReviewUpsertResult.success("리뷰를 저장했습니다.");
        } catch (IllegalArgumentException e) {
            recipeAuditPublisher.recipeReviewUpsertFailed(userId, recipeId, e.getMessage());
            return ReviewUpsertResult.failure(e.getMessage());
        }
    }

    public record RecipeDetailViewData(boolean bookmarked,
                                       List<String> categories,
                                       Recipe_INFO_ResponseDto infoResponseDto,
                                       List<Recipe_IRDNT_ResponseDto> irdntResponseDto,
                                       List<Recipe_CRSE_ResponseDto> crseResponseDto,
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
