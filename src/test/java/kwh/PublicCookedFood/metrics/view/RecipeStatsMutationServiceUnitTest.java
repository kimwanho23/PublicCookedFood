package kwh.PublicCookedFood.metrics.view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class RecipeStatsMutationServiceUnitTest {

    @Mock
    private RecipeStatsRepository recipeStatsRepository;

    @InjectMocks
    private RecipeStatsMutationService recipeStatsMutationService;

    @Test
    void adjustBookmarkCount_updatesExistingStats() {
        when(recipeStatsRepository.addBookmarks(10L, 1L)).thenReturn(1);

        recipeStatsMutationService.adjustBookmarkCount(10L, 1L);

        verify(recipeStatsRepository).addBookmarks(10L, 1L);
        verify(recipeStatsRepository, never()).save(any(RecipeStats.class));
    }

    @Test
    void adjustBookmarkCount_createsStatsWhenMissingAndDeltaPositive() {
        when(recipeStatsRepository.addBookmarks(11L, 1L)).thenReturn(0);

        recipeStatsMutationService.adjustBookmarkCount(11L, 1L);

        ArgumentCaptor<RecipeStats> captor = ArgumentCaptor.forClass(RecipeStats.class);
        verify(recipeStatsRepository).save(captor.capture());
        RecipeStats saved = captor.getValue();
        assertThat(saved.getRecipeId()).isEqualTo(11L);
        assertThat(saved.getTotalBookmarks()).isEqualTo(1L);
        assertThat(saved.getScore()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void adjustBookmarkCount_skipsCreateWhenMissingAndDeltaNegative() {
        when(recipeStatsRepository.addBookmarks(12L, -1L)).thenReturn(0);

        recipeStatsMutationService.adjustBookmarkCount(12L, -1L);

        verify(recipeStatsRepository, never()).save(any(RecipeStats.class));
    }

    @Test
    void adjustBookmarkCount_retriesWhenConcurrentInsertHappens() {
        when(recipeStatsRepository.addBookmarks(13L, 1L)).thenReturn(0, 1);
        when(recipeStatsRepository.save(any(RecipeStats.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        recipeStatsMutationService.adjustBookmarkCount(13L, 1L);

        verify(recipeStatsRepository, times(2)).addBookmarks(13L, 1L);
        verify(recipeStatsRepository).save(any(RecipeStats.class));
    }
}
