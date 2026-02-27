package kwh.PublicCookedFood.user.audit;

import kwh.PublicCookedFood.user.aop.UserActionAuditType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecipeAuditPublisher {

    private final AuditEventDispatcher dispatcher;

    public void recipeReviewUpsert(Long userId, Long recipeId, Integer rating) {
        dispatcher.publishUserAction(UserActionAuditType.RECIPE_REVIEW_UPSERT, userId, recipeId, rating);
    }

    public void recipeReviewUpsertFailed(Long userId, Long recipeId, String reason) {
        dispatcher.publishUserAction(UserActionAuditType.RECIPE_REVIEW_UPSERT_FAILED, userId, recipeId, reason);
    }
}
