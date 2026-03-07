package kwh.PublicCookedFood.account.audit;

import kwh.PublicCookedFood.account.aop.AccountActionAuditType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecipeAuditPublisher {

    private final AuditEventDispatcher dispatcher;

    public void recipeReviewUpsert(Long accountId, Long recipeId, Integer rating) {
        dispatcher.publishAccountAction(AccountActionAuditType.RECIPE_REVIEW_UPSERT, accountId, recipeId, rating);
    }

    public void recipeReviewUpsertFailed(Long accountId, Long recipeId, String reason) {
        dispatcher.publishAccountAction(AccountActionAuditType.RECIPE_REVIEW_UPSERT_FAILED, accountId, recipeId, reason);
    }
}

