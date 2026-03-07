package kwh.PublicCookedFood.metrics.reco;

import kwh.PublicCookedFood.food.repository.RecipeReviewRepository;
import kwh.PublicCookedFood.metrics.view.RecipeStats;
import kwh.PublicCookedFood.metrics.view.RecipeStatsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeStatsScoreServiceUnitTest {

    @Mock
    private RecipeStatsRepository recipeStatsRepository;

    @Mock
    private RecipeReviewRepository recipeReviewRepository;

    @InjectMocks
    private RecipeStatsScoreService recipeStatsScoreService;

    @Test
    void recalculateScore_updatesRecipeScore() {
        ReflectionTestUtils.setField(recipeStatsScoreService, "viewsWeight", new BigDecimal("1.00"));
        ReflectionTestUtils.setField(recipeStatsScoreService, "likesWeight", BigDecimal.ZERO);
        ReflectionTestUtils.setField(recipeStatsScoreService, "bookmarksWeight", new BigDecimal("1.00"));
        ReflectionTestUtils.setField(recipeStatsScoreService, "freshnessWeight", BigDecimal.ZERO);
        ReflectionTestUtils.setField(recipeStatsScoreService, "freshnessLambda", new BigDecimal("0.03"));

        RecipeStats recipeStats = RecipeStats.builder()
                .recipeId(5L)
                .totalViews(9L)
                .totalLikes(0L)
                .totalBookmarks(3L)
                .score(BigDecimal.ZERO)
                .updatedAt(LocalDateTime.now())
                .build();
        when(recipeStatsRepository.findById(5L)).thenReturn(Optional.of(recipeStats));
        when(recipeReviewRepository.findLatestReviewTimeByRecipeId(5L)).thenReturn(Optional.empty());

        recipeStatsScoreService.recalculateScore(5L);

        ArgumentCaptor<RecipeStats> captor = ArgumentCaptor.forClass(RecipeStats.class);
        verify(recipeStatsRepository).save(captor.capture());
        assertThat(captor.getValue().getScore()).isEqualByComparingTo("3.688879");
    }

    @Test
    void recalculateScore_skipsWhenStatsMissing() {
        when(recipeStatsRepository.findById(6L)).thenReturn(Optional.empty());

        recipeStatsScoreService.recalculateScore(6L);

        verify(recipeStatsRepository, never()).save(org.mockito.ArgumentMatchers.any(RecipeStats.class));
    }
}
