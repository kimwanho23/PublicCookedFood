package kwh.PublicCookedFood.user.aop.recovery;

import kwh.PublicCookedFood.user.aop.AccountRecoveryAuditType;
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
            Long userId = args.asLong(0);
            log.info("action=user.account_reset_password result=success userId={}", userId);
            recorder.record(userId, "USER_ACCOUNT_RESET_PASSWORD", null);
        });
        handlers.put(AccountRecoveryAuditType.RESET_PASSWORD_NOT_FOUND,
                args -> log.info("action=user.account_reset_password result=not_found"));

        return handlers;
    }
}
