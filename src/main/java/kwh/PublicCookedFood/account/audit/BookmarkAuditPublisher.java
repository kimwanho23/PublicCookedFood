package kwh.PublicCookedFood.account.audit;

import kwh.PublicCookedFood.account.aop.AccountActionAuditType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookmarkAuditPublisher {

    private final AuditEventDispatcher dispatcher;

    public void bookmarkAdd(Long accountId, Long recipeId) {
        dispatcher.publishAccountAction(AccountActionAuditType.BOOKMARK_ADD, accountId, recipeId);
    }

    public void bookmarkRemove(Long accountId, Long recipeId) {
        dispatcher.publishAccountAction(AccountActionAuditType.BOOKMARK_REMOVE, accountId, recipeId);
    }

    public void bookmarkAddFailed(Long accountId, Long recipeId, String reason) {
        dispatcher.publishAccountAction(AccountActionAuditType.BOOKMARK_ADD_FAILED, accountId, recipeId, reason);
    }

    public void bookmarkRemoveFailed(Long accountId, Long recipeId, String reason) {
        dispatcher.publishAccountAction(AccountActionAuditType.BOOKMARK_REMOVE_FAILED, accountId, recipeId, reason);
    }
}

