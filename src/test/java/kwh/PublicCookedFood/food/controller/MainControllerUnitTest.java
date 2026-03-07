package kwh.PublicCookedFood.food.controller;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.food.dto.response.RecipeRankingResponse;
import kwh.PublicCookedFood.food.facade.RecipeMainFacade;
import kwh.PublicCookedFood.metrics.reco.RecipeRecoSnapshotService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MainControllerUnitTest {

    @Mock
    private RecipeMainFacade recipeMainFacade;

    @InjectMocks
    private MainController mainController;

    @Test
    void home_setsPopularAndRecommendationModelAttributes() {
        Board popularBoard = Board.builder()
                .id(1L)
                .title("popular")
                .state(SoftDeleteState.ACTIVE)
                .hiddenByReport(false)
                .build();
        RecipeRankingResponse ranking = new RecipeRankingResponse(10L, "ranking", 4L, 4.8);
        RecipeRecoSnapshotService.RecipeRecommendationItem lunchItem =
                new RecipeRecoSnapshotService.RecipeRecommendationItem(20L, "lunch", "/lunch.png", BigDecimal.valueOf(8.5));
        RecipeRecoSnapshotService.RecipeRecommendationItem dinnerItem =
                new RecipeRecoSnapshotService.RecipeRecommendationItem(30L, "dinner", "/dinner.png", BigDecimal.valueOf(7.2));
        RecipeMainFacade.HomeViewData homeData = new RecipeMainFacade.HomeViewData(
                List.of(popularBoard),
                List.of(ranking),
                List.of(lunchItem),
                List.of(dinnerItem)
        );
        when(recipeMainFacade.loadHomeData()).thenReturn(homeData);

        Model model = new ExtendedModelMap();
        String viewName = mainController.home(model);

        assertThat(viewName).isEqualTo("/foods/main");
        assertThat(model.getAttribute("popularBoards")).isEqualTo(homeData.popularBoards());
        assertThat(model.getAttribute("recipeRankings")).isEqualTo(homeData.recipeRankings());
        assertThat(model.getAttribute("lunchRecommendations")).isEqualTo(homeData.lunchRecommendations());
        assertThat(model.getAttribute("dinnerRecommendations")).isEqualTo(homeData.dinnerRecommendations());
    }
}
