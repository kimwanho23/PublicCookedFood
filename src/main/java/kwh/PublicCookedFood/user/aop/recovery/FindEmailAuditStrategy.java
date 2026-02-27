package kwh.PublicCookedFood.user.aop.recovery;

import kwh.PublicCookedFood.user.aop.AccountRecoveryAuditType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class FindEmailAuditStrategy extends AbstractAccountRecoveryAuditStrategy {

    public FindEmailAuditStrategy(AccountRecoveryAuditRecorder recorder) {
        super(createHandlers(recorder));
    }

    private static Map<AccountRecoveryAuditType, Consumer<AccountRecoveryAuditArgs>> createHandlers(
            AccountRecoveryAuditRecorder recorder) {
        Map<AccountRecoveryAuditType, Consumer<AccountRecoveryAuditArgs>> handlers = newHandlers();

        handlers.put(AccountRecoveryAuditType.FIND_EMAIL_SUCCESS, args -> {
            Long userId = args.asLong(0);
            log.info("action=user.account_recover_email result=success userId={}", userId);
            recorder.record(userId, "USER_ACCOUNT_FIND_EMAIL", null);
        });
        handlers.put(AccountRecoveryAuditType.FIND_EMAIL_NOT_FOUND,
                args -> log.info("action=user.account_recover_email result=not_found"));

        return handlers;
    }
}
