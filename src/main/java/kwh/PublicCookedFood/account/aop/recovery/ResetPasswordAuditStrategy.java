package kwh.PublicCookedFood.account.aop.recovery;

import kwh.PublicCookedFood.account.aop.AccountRecoveryAuditType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class ResetPasswordAuditStrategy extends AbstractAccountRecoveryAuditStrategy {

    public ResetPasswordAuditStrategy(AccountRecoveryAuditRecorder recorder) {
        super(createHandlers(recorder));
    }

    private static Map<AccountRecoveryAuditType, Consumer<AccountRecoveryAuditArgs>> createHandlers(
            AccountRecoveryAuditRecorder recorder) {
        Map<AccountRecoveryAuditType, Consumer<AccountRecoveryAuditArgs>> handlers = newHandlers();

        handlers.put(AccountRecoveryAuditType.RESET_PASSWORD_SUCCESS, args -> {
            Long accountId = args.asLong(0);
            log.info("action=account.account_reset_password result=success accountId={}", args.displayId(accountId));
            recorder.record(accountId, "ACCOUNT_ACCOUNT_RESET_PASSWORD", null);
        });
        handlers.put(AccountRecoveryAuditType.RESET_PASSWORD_NOT_FOUND,
                args -> log.info("action=account.account_reset_password result=not_found"));

        return handlers;
    }
}

