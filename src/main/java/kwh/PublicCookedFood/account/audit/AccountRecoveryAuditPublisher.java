package kwh.PublicCookedFood.account.audit;

import kwh.PublicCookedFood.account.aop.AccountRecoveryAuditType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountRecoveryAuditPublisher {

    private final AuditEventDispatcher dispatcher;

    public void findEmailSuccess(Long accountId) {
        dispatcher.publishAccountRecovery(AccountRecoveryAuditType.FIND_EMAIL_SUCCESS, accountId);
    }

    public void findEmailNotFound() {
        dispatcher.publishAccountRecovery(AccountRecoveryAuditType.FIND_EMAIL_NOT_FOUND);
    }

    public void resetPasswordSuccess(Long accountId) {
        dispatcher.publishAccountRecovery(AccountRecoveryAuditType.RESET_PASSWORD_SUCCESS, accountId);
    }

    public void resetPasswordNotFound() {
        dispatcher.publishAccountRecovery(AccountRecoveryAuditType.RESET_PASSWORD_NOT_FOUND);
    }

    public void resetPasswordCodeMailUnavailable(String email, Throwable throwable) {
        dispatcher.publishAccountRecovery(AccountRecoveryAuditType.RESET_PASSWORD_CODE_MAIL_UNAVAILABLE, email, throwable);
    }

    public void resetPasswordCodeIssued(String email) {
        dispatcher.publishAccountRecovery(AccountRecoveryAuditType.RESET_PASSWORD_CODE_ISSUED, email);
    }

    public void resetPasswordCodeNotFoundOrSkipped(String email) {
        dispatcher.publishAccountRecovery(AccountRecoveryAuditType.RESET_PASSWORD_CODE_NOT_FOUND_OR_SKIPPED, email);
    }
}

