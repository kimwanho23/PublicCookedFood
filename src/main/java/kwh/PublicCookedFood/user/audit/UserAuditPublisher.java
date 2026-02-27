package kwh.PublicCookedFood.user.audit;

import kwh.PublicCookedFood.user.aop.UserActionAuditType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAuditPublisher {

    private final AuditEventDispatcher dispatcher;

    public void userSignup(Long userId, String email) {
        dispatcher.publishUserAction(UserActionAuditType.USER_SIGNUP, userId, email);
    }

    public void userProfileUpdate(Long userId) {
        dispatcher.publishUserAction(UserActionAuditType.USER_PROFILE_UPDATE, userId);
    }

    public void userProfileUpdateFailedUnauthenticated() {
        dispatcher.publishUserAction(UserActionAuditType.USER_PROFILE_UPDATE_FAILED_UNAUTHENTICATED);
    }

    public void userProfileUpdateFailedValidation(Long userId) {
        dispatcher.publishUserAction(UserActionAuditType.USER_PROFILE_UPDATE_FAILED_VALIDATION, userId);
    }

    public void userProfileUpdateFailedUserNotFound(Long userId) {
        dispatcher.publishUserAction(UserActionAuditType.USER_PROFILE_UPDATE_FAILED_USER_NOT_FOUND, userId);
    }

    public void userProfileUpdateFailed(Long userId, String reason) {
        dispatcher.publishUserAction(UserActionAuditType.USER_PROFILE_UPDATE_FAILED, userId, reason);
    }

    public void userBlockAdd(Long blockerId, Long targetUserId) {
        dispatcher.publishUserAction(UserActionAuditType.USER_BLOCK_ADD, blockerId, targetUserId);
    }

    public void userBlockRemove(Long blockerId, Long targetUserId) {
        dispatcher.publishUserAction(UserActionAuditType.USER_BLOCK_REMOVE, blockerId, targetUserId);
    }

    public void userBlockSkippedDuplicate(Long blockerId, Long targetUserId) {
        dispatcher.publishUserAction(UserActionAuditType.USER_BLOCK_SKIPPED_DUPLICATE, blockerId, targetUserId);
    }

    public void userUnblockSkippedNotFound(Long blockerId, Long targetUserId) {
        dispatcher.publishUserAction(UserActionAuditType.USER_UNBLOCK_SKIPPED_NOT_FOUND, blockerId, targetUserId);
    }

    public void userBlockFailedUnauthenticated(Long targetUserId) {
        dispatcher.publishUserAction(UserActionAuditType.USER_BLOCK_FAILED_UNAUTHENTICATED, targetUserId);
    }

    public void userBlockFailed(Long blockerId, Long blockedId, String reason, Throwable throwable) {
        dispatcher.publishUserAction(UserActionAuditType.USER_BLOCK_FAILED, blockerId, blockedId, reason, throwable);
    }

    public void userUnblockFailedUnauthenticated(Long targetUserId) {
        dispatcher.publishUserAction(UserActionAuditType.USER_UNBLOCK_FAILED_UNAUTHENTICATED, targetUserId);
    }

    public void userUnblockFailed(Long blockerId, Long blockedId, String reason, Throwable throwable) {
        dispatcher.publishUserAction(UserActionAuditType.USER_UNBLOCK_FAILED, blockerId, blockedId, reason, throwable);
    }
}
