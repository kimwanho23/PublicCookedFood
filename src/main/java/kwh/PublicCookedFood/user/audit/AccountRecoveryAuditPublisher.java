package kwh.PublicCookedFood.user.audit;

import kwh.PublicCookedFood.user.aop.AccountRecoveryAuditType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountRecoveryAuditPublisher {

    private final AuditEventDispatcher dispatcher;

    public void findEmailSuccess(Long userId) {
        dispatcher.publishAccountRecovery(AccountRecoveryAuditType.FIND_EMAIL_SUCCESS, userId);
    }

    public void findEmailNotFound() {
        dispatcher.publishAccountRecovery(AccountRecoveryAuditType.FIND_EMAIL_NOT_FOUND);
    }

    public void resetPasswordSuccess(Long userId) {
        dispatcher.publishAccountRecovery(AccountRecoveryAuditType.RESET_PASSWORD_SUCCESS, userId);
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
