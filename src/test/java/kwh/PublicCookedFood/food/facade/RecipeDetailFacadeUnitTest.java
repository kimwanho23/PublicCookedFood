package kwh.PublicCookedFood.food.facade;

import kwh.PublicCookedFood.food.dto.response.RecipeReviewSummaryResponse;
import kwh.PublicCookedFood.food.dto.response.recipe_info.Recipe_INFO_ResponseDto;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.metrics.reco.RecipeScoreEventPublisher;
import kwh.PublicCookedFood.food.service.RecipeReviewService;
import kwh.PublicCookedFood.food.service.RecipeService;
import kwh.PublicCookedFood.metrics.view.RecipeViewCounterService;
import kwh.PublicCookedFood.account.audit.RecipeAuditPublisher;
import kwh.PublicCookedFood.account.service.BookmarkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class RecipeDetailFacadeUnitTest {

    @Mock
    private RecipeService recipeService;

    @Mock
    private BookmarkService bookmarkService;

    @Mock
    private RecipeReviewService recipeReviewService;

    @Mock
    private RecipeAuditPublisher recipeAuditPublisher;

    @Mock
    private RecipeViewCounterService recipeViewCounterService;

    @Mock
    private RecipeScoreEventPublisher recipeScoreEventPublisher;

    private RecipeDetailFacade recipeDetailFacade;

    @BeforeEach
    void setUp() {
        recipeDetailFacade = new RecipeDetailFacade(
                recipeService,
                bookmarkService,
                recipeReviewService,
                recipeAuditPublisher,
                recipeViewCounterService,
                recipeScoreEventPublisher
        );
    }

    @Test
    void loadRecipeDetail_increasesViewsWhenFlagIsTrue() {
        long recipeId = 10L;
        Recipe_INFO recipeInfo = Recipe_INFO.builder().recipeID(recipeId).recipeNMKO("김치찌개").build();
        Recipe_INFO_ResponseDto info = Recipe_INFO_ResponseDto.builder().recipeID(recipeId).recipeNMKO("김치찌개").build();

        when(recipeService.getRecipeEntityByRecipeId(recipeId)).thenReturn(recipeInfo);
        when(recipeService.toInfoResponse(recipeInfo)).thenReturn(info);
        when(recipeService.getRecipeIrdnt(recipeId)).thenReturn(List.of());
        when(recipeService.getRecipeCrse(recipeId)).thenReturn(List.of());
        when(recipeReviewService.getSummary(recipeId)).thenReturn(new RecipeReviewSummaryResponse(0, 0));
        when(recipeReviewService.getRecentReviews(recipeId)).thenReturn(List.of());
        when(recipeViewCounterService.increaseRecipeViewAndGet(recipeId)).thenReturn(15L);

        RecipeDetailFacade.RecipeDetailViewData result = recipeDetailFacade.loadRecipeDetail(recipeId, null, true);

        assertThat(result.viewCount()).isEqualTo(15L);
        verify(recipeViewCounterService).increaseRecipeViewAndGet(recipeId);
        verify(recipeViewCounterService, never()).getRecipeViewCount(anyLong());
    }

    @Test
    void loadRecipeDetail_skipsViewIncreaseWhenFlagIsFalse() {
        long recipeId = 11L;
        Recipe_INFO recipeInfo = Recipe_INFO.builder().recipeID(recipeId).recipeNMKO("된장찌개").build();
        Recipe_INFO_ResponseDto info = Recipe_INFO_ResponseDto.builder().recipeID(recipeId).recipeNMKO("된장찌개").build();

        when(recipeService.getRecipeEntityByRecipeId(recipeId)).thenReturn(recipeInfo);
        when(recipeService.toInfoResponse(recipeInfo)).thenReturn(info);
        when(recipeService.getRecipeIrdnt(recipeId)).thenReturn(List.of());
        when(recipeService.getRecipeCrse(recipeId)).thenReturn(List.of());
        when(recipeReviewService.getSummary(recipeId)).thenReturn(new RecipeReviewSummaryResponse(0, 0));
        when(recipeReviewService.getRecentReviews(recipeId)).thenReturn(List.of());
        when(recipeViewCounterService.getRecipeViewCount(recipeId)).thenReturn(20L);

        RecipeDetailFacade.RecipeDetailViewData result = recipeDetailFacade.loadRecipeDetail(recipeId, null, false);

        assertThat(result.viewCount()).isEqualTo(20L);
        verify(recipeViewCounterService).getRecipeViewCount(recipeId);
        verify(recipeViewCounterService, never()).increaseRecipeViewAndGet(anyLong());
    }

    @Test
    void upsertReview_recalculatesScoreWhenSuccess() {
        RecipeDetailFacade.ReviewUpsertResult result = recipeDetailFacade.upsertReview(21L, 3L, 5, "great");

        assertThat(result.success()).isTrue();
        verify(recipeReviewService).upsertReview(21L, 3L, 5, "great");
        verify(recipeScoreEventPublisher).publishRecalculateRequest(21L, "REVIEW_UPSERT");
        verify(recipeAuditPublisher).recipeReviewUpsert(3L, 21L, 5);
    }

    @Test
    void upsertReview_doesNotRecalculateWhenValidationFails() {
        doThrow(new IllegalArgumentException("bad request"))
                .when(recipeReviewService)
                .upsertReview(22L, 4L, 1, "x");

        RecipeDetailFacade.ReviewUpsertResult result = recipeDetailFacade.upsertReview(22L, 4L, 1, "x");

        assertThat(result.success()).isFalse();
        verify(recipeScoreEventPublisher, never()).publishRecalculateRequest(22L, "REVIEW_UPSERT");
        verify(recipeAuditPublisher).recipeReviewUpsertFailed(4L, 22L, "bad request");
    }
}
