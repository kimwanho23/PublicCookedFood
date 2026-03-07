package kwh.PublicCookedFood.account.aop.recovery;

import kwh.PublicCookedFood.account.aop.AccountRecoveryAuditType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class ResetPasswordCodeAuditStrategy extends AbstractAccountRecoveryAuditStrategy {

    public ResetPasswordCodeAuditStrategy() {
        super(createHandlers());
    }

    private static Map<AccountRecoveryAuditType, Consumer<AccountRecoveryAuditArgs>> createHandlers() {
        Map<AccountRecoveryAuditType, Consumer<AccountRecoveryAuditArgs>> handlers = newHandlers();

        handlers.put(AccountRecoveryAuditType.RESET_PASSWORD_CODE_MAIL_UNAVAILABLE, args -> {
            String email = args.asString(0);
            Throwable throwable = args.asThrowable(1);
            if (throwable == null) {
                log.warn("action=account.account_reset_password_code result=mail_unavailable email={}", email);
                return;
            }
            log.warn("action=account.account_reset_password_code result=mail_unavailable email={}", email, throwable);
        });
        handlers.put(AccountRecoveryAuditType.RESET_PASSWORD_CODE_ISSUED, args -> {
            String email = args.asString(0);
            log.info("action=account.account_reset_password_code result=issued email={}", email);
        });
        handlers.put(AccountRecoveryAuditType.RESET_PASSWORD_CODE_NOT_FOUND_OR_SKIPPED, args -> {
            String email = args.asString(0);
            log.info("action=account.account_reset_password_code result=not_found_or_skipped email={}", email);
        });

        return handlers;
    }
}
