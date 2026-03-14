package kwh.PublicCookedFood.account.aop.action;

import kwh.PublicCookedFood.account.aop.AccountActionAuditType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class AccountProfileAuditStrategy extends AbstractAccountActionAuditStrategy {

    public AccountProfileAuditStrategy(AccountActionAuditRecorder recorder) {
        super(createHandlers(recorder));
    }

    private static Map<AccountActionAuditType, Consumer<AccountActionAuditArgs>> createHandlers(
            AccountActionAuditRecorder recorder) {
        Map<AccountActionAuditType, Consumer<AccountActionAuditArgs>> handlers = newHandlers();

        handlers.put(AccountActionAuditType.ACCOUNT_SIGNUP, args -> {
            Long accountId = args.asLong(0);
            String email = args.asString(1);
            log.info("action=account.signup result=success accountId={} email={}",
                    args.displayId(accountId), args.displayText(email));
            recorder.record(accountId, "ACCOUNT_SIGNUP", "email=" + args.displayText(email));
        });
        handlers.put(AccountActionAuditType.ACCOUNT_PROFILE_UPDATE, args -> {
            Long accountId = args.asLong(0);
            log.info("action=account.profile_update result=success accountId={}", args.displayId(accountId));
            recorder.record(accountId, "ACCOUNT_PROFILE_UPDATE", null);
        });
        handlers.put(AccountActionAuditType.ACCOUNT_PROFILE_UPDATE_FAILED_UNAUTHENTICATED,
                args -> log.warn("action=account.profile_update result=failed reason=unauthenticated"));
        handlers.put(AccountActionAuditType.ACCOUNT_PROFILE_UPDATE_FAILED_VALIDATION, args -> {
            Long accountId = args.asLong(0);
            log.warn("action=account.profile_update result=failed reason=validation_error accountId={}",
                    args.displayId(accountId));
        });
        handlers.put(AccountActionAuditType.ACCOUNT_PROFILE_UPDATE_FAILED_USER_NOT_FOUND, args -> {
            Long accountId = args.asLong(0);
            log.warn("action=account.profile_update result=failed reason=account_not_found accountId={}",
                    args.displayId(accountId));
        });
        handlers.put(AccountActionAuditType.ACCOUNT_PROFILE_UPDATE_FAILED, args -> {
            Long accountId = args.asLong(0);
            String reason = args.asString(1);
            log.warn("action=account.profile_update result=failed accountId={} reason={}",
                    args.displayId(accountId), args.displayText(reason));
        });

        return handlers;
    }
}

