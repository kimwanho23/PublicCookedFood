package kwh.PublicCookedFood.metrics.reco;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecipeScoreRecalculateEventListenerUnitTest {

    @Mock
    private RecipeStatsScoreService recipeStatsScoreService;

    @InjectMocks
    private RecipeScoreRecalculateEventListener listener;

    @Test
    void handle_recalculatesScoreWhenRecipeIdExists() {
        listener.handle(new RecipeScoreRecalculateRequestedEvent(100L, "VIEW_INCREMENT"));

        verify(recipeStatsScoreService).recalculateScore(100L);
    }

    @Test
    void handle_ignoresWhenRecipeIdMissing() {
        listener.handle(new RecipeScoreRecalculateRequestedEvent(null, "UNKNOWN"));

        verify(recipeStatsScoreService, never()).recalculateScore(any());
    }
}
