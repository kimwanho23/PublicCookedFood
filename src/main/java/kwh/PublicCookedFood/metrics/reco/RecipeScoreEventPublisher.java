package kwh.PublicCookedFood.metrics.reco;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RecipeScoreEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishRecalculateRequest(Long recipeId, String reason) {
        if (recipeId == null) {
            return;
        }
        applicationEventPublisher.publishEvent(new RecipeScoreRecalculateRequestedEvent(recipeId, reason));
    }
}
