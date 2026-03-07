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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeStatsScoreSchedulerUnitTest {

    @Mock
    private RecipeStatsRepository recipeStatsRepository;

    @Mock
    private RecipeReviewRepository recipeReviewRepository;

    @Mock
    private RecipeStatsScoreService recipeStatsScoreService;

    @InjectMocks
    private RecipeStatsScoreScheduler scheduler;

    @Test
    void recalculateRecipeScores_returnsImmediatelyWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "enabled", false);

        scheduler.recalculateRecipeScores();

        verify(recipeStatsRepository, never()).findByRecipeIdGreaterThanOrderByRecipeIdAsc(any(), any(PageRequest.class));
    }

    @Test
    void recalculateRecipeScores_updatesScoreFromStatsCounters() {
        ReflectionTestUtils.setField(scheduler, "enabled", true);
        ReflectionTestUtils.setField(scheduler, "batchSize", 2);

        RecipeStats stats1 = RecipeStats.builder()
                .recipeId(1L)
                .totalViews(9L)
                .totalLikes(0L)
                .totalBookmarks(0L)
                .score(BigDecimal.ZERO)
                .updatedAt(LocalDateTime.now())
                .build();
        RecipeStats stats2 = RecipeStats.builder()
                .recipeId(2L)
                .totalViews(0L)
                .totalLikes(0L)
                .totalBookmarks(5L)
                .score(BigDecimal.ONE)
                .updatedAt(LocalDateTime.now())
                .build();

        when(recipeReviewRepository.findLatestReviewTimeByRecipeId()).thenReturn(List.of());
        when(recipeStatsRepository.findByRecipeIdGreaterThanOrderByRecipeIdAsc(-1L, PageRequest.of(0, 2)))
                .thenReturn(List.of(stats1, stats2));
        when(recipeStatsRepository.findByRecipeIdGreaterThanOrderByRecipeIdAsc(2L, PageRequest.of(0, 2)))
                .thenReturn(List.of());
        when(recipeStatsScoreService.calculateScore(any(RecipeStats.class), any(), any()))
                .thenReturn(new BigDecimal("3.5"), new BigDecimal("1.7"));

        scheduler.recalculateRecipeScores();

        ArgumentCaptor<List<RecipeStats>> captor = ArgumentCaptor.forClass(List.class);
        verify(recipeStatsRepository).saveAll(captor.capture());
        List<RecipeStats> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getRecipeId()).isEqualTo(1L);
        assertThat(saved.get(0).getScore()).isEqualByComparingTo("3.500000");
        assertThat(saved.get(1).getRecipeId()).isEqualTo(2L);
        assertThat(saved.get(1).getScore()).isEqualByComparingTo("1.700000");
    }
}
