package kwh.PublicCookedFood.food.facade;

import kwh.PublicCookedFood.board.service.BoardService;
import kwh.PublicCookedFood.food.dto.response.RecipeRankingResponse;
import kwh.PublicCookedFood.food.service.RecipeReviewService;
import kwh.PublicCookedFood.food.service.RecipeService;
import kwh.PublicCookedFood.metrics.reco.RecipeRecoSnapshotService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeMainFacadeUnitTest {

    @Mock
    private RecipeService recipeService;

    @Mock
    private BoardService boardService;

    @Mock
    private RecipeReviewService recipeReviewService;

    @Mock
    private RecipeRecoSnapshotService recipeRecoSnapshotService;

    @InjectMocks
    private RecipeMainFacade recipeMainFacade;

    @Test
    void loadHomeData_includesLunchAndDinnerRecommendations() {
        when(recipeReviewService.getTopReviewRankings(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(6)))
                .thenReturn(List.of(new RecipeRankingResponse(1L, "rank", 10L, 4.5)));
        when(recipeRecoSnapshotService.loadRecommendations(RecipeRecoSnapshotService.SLOT_LUNCH, 6))
                .thenReturn(List.of(new RecipeRecoSnapshotService.RecipeRecommendationItem(2L, "lunch", "/l.png", BigDecimal.ONE)));
        when(recipeRecoSnapshotService.loadRecommendations(RecipeRecoSnapshotService.SLOT_DINNER, 6))
                .thenReturn(List.of(new RecipeRecoSnapshotService.RecipeRecommendationItem(3L, "dinner", "/d.png", BigDecimal.TEN)));

        RecipeMainFacade.HomeViewData result = recipeMainFacade.loadHomeData();

        assertThat(result.lunchRecommendations()).hasSize(1);
        assertThat(result.dinnerRecommendations()).hasSize(1);
        assertThat(result.recipeRankings()).hasSize(1);
    }
}
