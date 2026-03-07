package kwh.PublicCookedFood.account.aop.action;

import kwh.PublicCookedFood.account.aop.AccountActionAuditType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class BookmarkAuditStrategy extends AbstractAccountActionAuditStrategy {

    public BookmarkAuditStrategy(AccountActionAuditRecorder recorder) {
        super(createHandlers(recorder));
    }

    private static Map<AccountActionAuditType, Consumer<AccountActionAuditArgs>> createHandlers(
            AccountActionAuditRecorder recorder) {
        Map<AccountActionAuditType, Consumer<AccountActionAuditArgs>> handlers = newHandlers();

        handlers.put(AccountActionAuditType.BOOKMARK_ADD, args -> {
            Long accountId = args.asLong(0);
            Long recipeId = args.asLong(1);
            log.info("action=bookmark.add result=success accountId={} recipeId={}", accountId, recipeId);
            recorder.record(accountId, "BOOKMARK_ADD", "recipeId=" + args.safeId(recipeId));
        });
        handlers.put(AccountActionAuditType.BOOKMARK_REMOVE, args -> {
            Long accountId = args.asLong(0);
            Long recipeId = args.asLong(1);
            log.info("action=bookmark.remove result=success accountId={} recipeId={}", accountId, recipeId);
            recorder.record(accountId, "BOOKMARK_REMOVE", "recipeId=" + args.safeId(recipeId));
        });
        handlers.put(AccountActionAuditType.BOOKMARK_ADD_FAILED, args -> {
            Long accountId = args.asLong(0);
            Long recipeId = args.asLong(1);
            String reason = args.asString(2);
            log.warn("action=bookmark.add result=failed accountId={} recipeId={} reason={}", accountId, recipeId, reason);
        });
        handlers.put(AccountActionAuditType.BOOKMARK_REMOVE_FAILED, args -> {
            Long accountId = args.asLong(0);
            Long recipeId = args.asLong(1);
            String reason = args.asString(2);
            log.warn("action=bookmark.remove result=failed accountId={} recipeId={} reason={}", accountId, recipeId, reason);
        });

        return handlers;
    }
}

