package kwh.PublicCookedFood.account.aop.action;

import kwh.PublicCookedFood.account.aop.AccountActionAuditType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class AccountBlockAuditStrategy extends AbstractAccountActionAuditStrategy {

    public AccountBlockAuditStrategy(AccountActionAuditRecorder recorder) {
        super(createHandlers(recorder));
    }

    private static Map<AccountActionAuditType, Consumer<AccountActionAuditArgs>> createHandlers(
            AccountActionAuditRecorder recorder) {
        Map<AccountActionAuditType, Consumer<AccountActionAuditArgs>> handlers = newHandlers();

        handlers.put(AccountActionAuditType.ACCOUNT_BLOCK_ADD, args -> recordStateChange(args, recorder,
                "action=account.block result=success blockerId={} blockedId={}",
                "ACCOUNT_BLOCK_ADD"));
        handlers.put(AccountActionAuditType.ACCOUNT_BLOCK_REMOVE, args -> recordStateChange(args, recorder,
                "action=account.unblock result=success blockerId={} blockedId={}",
                "ACCOUNT_BLOCK_REMOVE"));
        handlers.put(AccountActionAuditType.ACCOUNT_BLOCK_SKIPPED_DUPLICATE, args -> {
            Long blockerId = args.asLong(0);
            Long targetAccountId = args.asLong(1);
            log.info("action=account.block result=skipped_duplicate blockerId={} blockedId={}", blockerId, targetAccountId);
        });
        handlers.put(AccountActionAuditType.ACCOUNT_UNBLOCK_SKIPPED_NOT_FOUND, args -> {
            Long blockerId = args.asLong(0);
            Long targetAccountId = args.asLong(1);
            log.info("action=account.unblock result=skipped_not_found blockerId={} blockedId={}", blockerId, targetAccountId);
        });
        handlers.put(AccountActionAuditType.ACCOUNT_BLOCK_FAILED_UNAUTHENTICATED, args -> {
            Long targetAccountId = args.asLong(0);
            log.warn("action=account.block result=failed reason=unauthenticated targetAccountId={}", targetAccountId);
        });
        handlers.put(AccountActionAuditType.ACCOUNT_BLOCK_FAILED,
                args -> logWarnWithOptionalThrowable("action=account.block result=failed blockerId={} blockedId={} reason={}", args));
        handlers.put(AccountActionAuditType.ACCOUNT_UNBLOCK_FAILED_UNAUTHENTICATED, args -> {
            Long targetAccountId = args.asLong(0);
            log.warn("action=account.unblock result=failed reason=unauthenticated targetAccountId={}", targetAccountId);
        });
        handlers.put(AccountActionAuditType.ACCOUNT_UNBLOCK_FAILED,
                args -> logWarnWithOptionalThrowable("action=account.unblock result=failed blockerId={} blockedId={} reason={}", args));

        return handlers;
    }

    private static void recordStateChange(
            AccountActionAuditArgs args,
            AccountActionAuditRecorder recorder,
            String logMessage,
            String activityType) {
        Long blockerId = args.asLong(0);
        Long targetAccountId = args.asLong(1);
        log.info(logMessage, blockerId, targetAccountId);
        recorder.record(blockerId, activityType, "targetAccountId=" + args.safeId(targetAccountId));
    }

    private static void logWarnWithOptionalThrowable(String message, AccountActionAuditArgs args) {
        Long blockerId = args.asLong(0);
        Long blockedId = args.asLong(1);
        String reason = args.asString(2);
        Throwable throwable = args.asThrowable(3);
        if (throwable == null) {
            log.warn(message, blockerId, blockedId, reason);
            return;
        }
        log.warn(message, blockerId, blockedId, reason, throwable);
    }
}

