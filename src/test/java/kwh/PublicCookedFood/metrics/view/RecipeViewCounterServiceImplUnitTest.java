package kwh.PublicCookedFood.metrics.view;

import kwh.PublicCookedFood.metrics.reco.RecipeScoreEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeViewCounterServiceImplUnitTest {

    @Mock
    private RecipeStatsRepository recipeStatsRepository;

    @Mock
    private RecipeScoreEventPublisher recipeScoreEventPublisher;

    @InjectMocks
    private RecipeViewCounterServiceImpl recipeViewCounterService;

    @Test
    void increaseRecipeViewAndGet_insertsWhenStatsMissing() {
        when(recipeStatsRepository.addViews(100L, 1L)).thenReturn(0);
        when(recipeStatsRepository.findTotalViewsByRecipeId(100L)).thenReturn(Optional.of(1L));

        long result = recipeViewCounterService.increaseRecipeViewAndGet(100L);

        assertThat(result).isEqualTo(1L);
        verify(recipeStatsRepository).save(any(RecipeStats.class));
        verify(recipeScoreEventPublisher).publishRecalculateRequest(100L, "VIEW_INCREMENT");
    }

    @Test
    void increaseRecipeViewAndGet_updatesWhenStatsExists() {
        when(recipeStatsRepository.addViews(101L, 1L)).thenReturn(1);
        when(recipeStatsRepository.findTotalViewsByRecipeId(101L)).thenReturn(Optional.of(9L));

        long result = recipeViewCounterService.increaseRecipeViewAndGet(101L);

        assertThat(result).isEqualTo(9L);
        verify(recipeStatsRepository, never()).save(any(RecipeStats.class));
        verify(recipeScoreEventPublisher).publishRecalculateRequest(101L, "VIEW_INCREMENT");
    }

    @Test
    void getRecipeViewCount_returnsZeroWhenNotFound() {
        when(recipeStatsRepository.findTotalViewsByRecipeId(999L)).thenReturn(Optional.empty());

        long result = recipeViewCounterService.getRecipeViewCount(999L);

        assertThat(result).isZero();
    }
}
