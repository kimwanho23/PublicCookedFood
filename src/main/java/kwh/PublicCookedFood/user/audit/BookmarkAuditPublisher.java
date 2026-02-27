package kwh.PublicCookedFood.user.audit;

import kwh.PublicCookedFood.user.aop.UserActionAuditType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookmarkAuditPublisher {

    private final AuditEventDispatcher dispatcher;

    public void bookmarkAdd(Long userId, Long recipeId) {
        dispatcher.publishUserAction(UserActionAuditType.BOOKMARK_ADD, userId, recipeId);
    }

    public void bookmarkRemove(Long userId, Long recipeId) {
        dispatcher.publishUserAction(UserActionAuditType.BOOKMARK_REMOVE, userId, recipeId);
    }

    public void bookmarkAddFailed(Long userId, Long recipeId, String reason) {
        dispatcher.publishUserAction(UserActionAuditType.BOOKMARK_ADD_FAILED, userId, recipeId, reason);
    }

    public void bookmarkRemoveFailed(Long userId, Long recipeId, String reason) {
        dispatcher.publishUserAction(UserActionAuditType.BOOKMARK_REMOVE_FAILED, userId, recipeId, reason);
    }
}
