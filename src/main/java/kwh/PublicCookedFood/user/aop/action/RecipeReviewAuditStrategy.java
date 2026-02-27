package kwh.PublicCookedFood.user.aop.action;

import kwh.PublicCookedFood.user.aop.UserActionAuditType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class RecipeReviewAuditStrategy extends AbstractUserActionAuditStrategy {

    public RecipeReviewAuditStrategy(UserActionAuditRecorder recorder) {
        super(createHandlers(recorder));
    }

    private static Map<UserActionAuditType, Consumer<UserActionAuditArgs>> createHandlers(
            UserActionAuditRecorder recorder) {
        Map<UserActionAuditType, Consumer<UserActionAuditArgs>> handlers = newHandlers();

        handlers.put(UserActionAuditType.RECIPE_REVIEW_UPSERT, args -> {
            Long userId = args.asLong(0);
            Long recipeId = args.asLong(1);
            Integer rating = args.asInteger(2);
            log.info("action=recipe.review_upsert result=success userId={} recipeId={} rating={}", userId, recipeId, rating);
            recorder.record(userId, "RECIPE_REVIEW_UPSERT",
                    "recipeId=" + args.safeId(recipeId) + ",rating=" + args.safeNumber(rating));
        });
        handlers.put(UserActionAuditType.RECIPE_REVIEW_UPSERT_FAILED, args -> {
            Long userId = args.asLong(0);
            Long recipeId = args.asLong(1);
            String reason = args.asString(2);
            log.warn("action=recipe.review_upsert result=failed userId={} recipeId={} reason={}", userId, recipeId, reason);
        });

        return handlers;
    }
}
