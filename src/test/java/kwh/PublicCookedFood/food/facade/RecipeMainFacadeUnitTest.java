package kwh.PublicCookedFood.food.facade;

import kwh.PublicCookedFood.board.application.query.BoardCardViewAssembler;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.service.query.BoardPopularityQueryService;
import kwh.PublicCookedFood.board.service.support.BoardStatsSummary;
import kwh.PublicCookedFood.board.service.support.BoardStatsSummaryResolver;
import kwh.PublicCookedFood.food.dto.response.RecipeRankingResponse;
import kwh.PublicCookedFood.food.service.RecipeReviewService;
import kwh.PublicCookedFood.food.service.RecipeService;
import kwh.PublicCookedFood.metrics.reco.RecipeRecoSnapshotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private BoardPopularityQueryService boardPopularityQueryService;

    @Mock
    private RecipeReviewService recipeReviewService;

    @Mock
    private RecipeRecoSnapshotService recipeRecoSnapshotService;
    @Mock
    private BoardStatsSummaryResolver boardStatsSummaryResolver;

    private RecipeMainFacade recipeMainFacade;

    @BeforeEach
    void setUp() {
        recipeMainFacade = new RecipeMainFacade(
                recipeService,
                boardPopularityQueryService,
                recipeReviewService,
                recipeRecoSnapshotService,
                boardStatsSummaryResolver,
                new BoardCardViewAssembler()
        );
    }

    @Test
    void loadHomeData_includesLunchAndDinnerRecommendations() {
        Board popularBoard = Board.builder()
                .id(10L)
                .title("popular")
                .build();
        when(boardPopularityQueryService.getPopularBoardsSince(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(6)))
                .thenReturn(List.of(popularBoard));
        when(boardStatsSummaryResolver.resolve(List.of(popularBoard)))
                .thenReturn(java.util.Map.of(10L, new BoardStatsSummary(7L, 5L, 3L)));
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
        assertThat(result.popularBoards()).hasSize(1);
        assertThat(result.popularBoards().get(0).boardId()).isEqualTo(10L);
        assertThat(result.popularBoards().get(0).likes()).isEqualTo(5L);
    }
}
