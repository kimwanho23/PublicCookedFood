package kwh.PublicCookedFood.user.aop.action;

import kwh.PublicCookedFood.user.aop.UserActionAuditType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class BookmarkAuditStrategy extends AbstractUserActionAuditStrategy {

    public BookmarkAuditStrategy(UserActionAuditRecorder recorder) {
        super(createHandlers(recorder));
    }

    private static Map<UserActionAuditType, Consumer<UserActionAuditArgs>> createHandlers(
            UserActionAuditRecorder recorder) {
        Map<UserActionAuditType, Consumer<UserActionAuditArgs>> handlers = newHandlers();

        handlers.put(UserActionAuditType.BOOKMARK_ADD, args -> {
            Long userId = args.asLong(0);
            Long recipeId = args.asLong(1);
            log.info("action=bookmark.add result=success userId={} recipeId={}", userId, recipeId);
            recorder.record(userId, "BOOKMARK_ADD", "recipeId=" + args.safeId(recipeId));
        });
        handlers.put(UserActionAuditType.BOOKMARK_REMOVE, args -> {
            Long userId = args.asLong(0);
            Long recipeId = args.asLong(1);
            log.info("action=bookmark.remove result=success userId={} recipeId={}", userId, recipeId);
            recorder.record(userId, "BOOKMARK_REMOVE", "recipeId=" + args.safeId(recipeId));
        });
        handlers.put(UserActionAuditType.BOOKMARK_ADD_FAILED, args -> {
            Long userId = args.asLong(0);
            Long recipeId = args.asLong(1);
            String reason = args.asString(2);
            log.warn("action=bookmark.add result=failed userId={} recipeId={} reason={}", userId, recipeId, reason);
        });
        handlers.put(UserActionAuditType.BOOKMARK_REMOVE_FAILED, args -> {
            Long userId = args.asLong(0);
            Long recipeId = args.asLong(1);
            String reason = args.asString(2);
            log.warn("action=bookmark.remove result=failed userId={} recipeId={} reason={}", userId, recipeId, reason);
        });

        return handlers;
    }
}
