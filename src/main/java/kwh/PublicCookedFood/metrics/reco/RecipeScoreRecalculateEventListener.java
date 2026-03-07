package kwh.PublicCookedFood.metrics.reco;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecipeScoreRecalculateEventListener {

    private final RecipeStatsScoreService recipeStatsScoreService;

    @Async("recipeScoreEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(RecipeScoreRecalculateRequestedEvent event) {
        if (event == null || event.recipeId() == null) {
            return;
        }
        try {
            recipeStatsScoreService.recalculateScore(event.recipeId());
        } catch (RuntimeException e) {
            log.error("Failed to process recipe score recalculate event. recipeId={}, reason={}",
                    event.recipeId(),
                    event.reason(),
                    e);
        }
    }
}
