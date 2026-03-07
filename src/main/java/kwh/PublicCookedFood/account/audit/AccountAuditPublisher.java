package kwh.PublicCookedFood.account.audit;

import kwh.PublicCookedFood.account.aop.AccountActionAuditType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountAuditPublisher {

    private final AuditEventDispatcher dispatcher;

    public void accountSignup(Long accountId, String email) {
        dispatcher.publishAccountAction(AccountActionAuditType.ACCOUNT_SIGNUP, accountId, email);
    }

    public void accountProfileUpdate(Long accountId) {
        dispatcher.publishAccountAction(AccountActionAuditType.ACCOUNT_PROFILE_UPDATE, accountId);
    }

    public void accountProfileUpdateFailedUnauthenticated() {
        dispatcher.publishAccountAction(AccountActionAuditType.ACCOUNT_PROFILE_UPDATE_FAILED_UNAUTHENTICATED);
    }

    public void accountProfileUpdateFailedValidation(Long accountId) {
        dispatcher.publishAccountAction(AccountActionAuditType.ACCOUNT_PROFILE_UPDATE_FAILED_VALIDATION, accountId);
    }

    public void accountProfileUpdateFailedAccountNotFound(Long accountId) {
        dispatcher.publishAccountAction(AccountActionAuditType.ACCOUNT_PROFILE_UPDATE_FAILED_USER_NOT_FOUND, accountId);
    }

    public void accountProfileUpdateFailed(Long accountId, String reason) {
        dispatcher.publishAccountAction(AccountActionAuditType.ACCOUNT_PROFILE_UPDATE_FAILED, accountId, reason);
    }

    public void accountBlockAdd(Long blockerId, Long targetAccountId) {
        dispatcher.publishAccountAction(AccountActionAuditType.ACCOUNT_BLOCK_ADD, blockerId, targetAccountId);
    }

    public void accountBlockRemove(Long blockerId, Long targetAccountId) {
        dispatcher.publishAccountAction(AccountActionAuditType.ACCOUNT_BLOCK_REMOVE, blockerId, targetAccountId);
    }

    public void accountBlockSkippedDuplicate(Long blockerId, Long targetAccountId) {
        dispatcher.publishAccountAction(AccountActionAuditType.ACCOUNT_BLOCK_SKIPPED_DUPLICATE, blockerId, targetAccountId);
    }

    public void accountUnblockSkippedNotFound(Long blockerId, Long targetAccountId) {
        dispatcher.publishAccountAction(AccountActionAuditType.ACCOUNT_UNBLOCK_SKIPPED_NOT_FOUND, blockerId, targetAccountId);
    }

    public void accountBlockFailedUnauthenticated(Long targetAccountId) {
        dispatcher.publishAccountAction(AccountActionAuditType.ACCOUNT_BLOCK_FAILED_UNAUTHENTICATED, targetAccountId);
    }

    public void accountBlockFailed(Long blockerId, Long blockedId, String reason, Throwable throwable) {
        dispatcher.publishAccountAction(AccountActionAuditType.ACCOUNT_BLOCK_FAILED, blockerId, blockedId, reason, throwable);
    }

    public void accountUnblockFailedUnauthenticated(Long targetAccountId) {
        dispatcher.publishAccountAction(AccountActionAuditType.ACCOUNT_UNBLOCK_FAILED_UNAUTHENTICATED, targetAccountId);
    }

    public void accountUnblockFailed(Long blockerId, Long blockedId, String reason, Throwable throwable) {
        dispatcher.publishAccountAction(AccountActionAuditType.ACCOUNT_UNBLOCK_FAILED, blockerId, blockedId, reason, throwable);
    }
}

