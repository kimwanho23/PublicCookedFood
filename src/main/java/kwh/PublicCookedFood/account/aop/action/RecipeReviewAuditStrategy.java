package kwh.PublicCookedFood.account.aop.action;

import kwh.PublicCookedFood.account.aop.AccountActionAuditType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class RecipeReviewAuditStrategy extends AbstractAccountActionAuditStrategy {

    public RecipeReviewAuditStrategy(AccountActionAuditRecorder recorder) {
        super(createHandlers(recorder));
    }

    private static Map<AccountActionAuditType, Consumer<AccountActionAuditArgs>> createHandlers(
            AccountActionAuditRecorder recorder) {
        Map<AccountActionAuditType, Consumer<AccountActionAuditArgs>> handlers = newHandlers();

        handlers.put(AccountActionAuditType.RECIPE_REVIEW_UPSERT, args -> {
            Long accountId = args.asLong(0);
            Long recipeId = args.asLong(1);
            Integer rating = args.asInteger(2);
            log.info("action=recipe.review_upsert result=success accountId={} recipeId={} rating={}", accountId, recipeId, rating);
            recorder.record(accountId, "RECIPE_REVIEW_UPSERT",
                    "recipeId=" + args.safeId(recipeId) + ",rating=" + args.safeNumber(rating));
        });
        handlers.put(AccountActionAuditType.RECIPE_REVIEW_UPSERT_FAILED, args -> {
            Long accountId = args.asLong(0);
            Long recipeId = args.asLong(1);
            String reason = args.asString(2);
            log.warn("action=recipe.review_upsert result=failed accountId={} recipeId={} reason={}", accountId, recipeId, reason);
        });

        return handlers;
    }
}

