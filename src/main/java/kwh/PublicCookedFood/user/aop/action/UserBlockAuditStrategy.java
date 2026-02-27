package kwh.PublicCookedFood.user.aop.action;

import kwh.PublicCookedFood.user.aop.UserActionAuditType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class UserBlockAuditStrategy extends AbstractUserActionAuditStrategy {

    public UserBlockAuditStrategy(UserActionAuditRecorder recorder) {
        super(createHandlers(recorder));
    }

    private static Map<UserActionAuditType, Consumer<UserActionAuditArgs>> createHandlers(
            UserActionAuditRecorder recorder) {
        Map<UserActionAuditType, Consumer<UserActionAuditArgs>> handlers = newHandlers();

        handlers.put(UserActionAuditType.USER_BLOCK_ADD, args -> recordStateChange(args, recorder,
                "action=user.block result=success blockerId={} blockedId={}",
                "USER_BLOCK_ADD"));
        handlers.put(UserActionAuditType.USER_BLOCK_REMOVE, args -> recordStateChange(args, recorder,
                "action=user.unblock result=success blockerId={} blockedId={}",
                "USER_BLOCK_REMOVE"));
        handlers.put(UserActionAuditType.USER_BLOCK_SKIPPED_DUPLICATE, args -> {
            Long blockerId = args.asLong(0);
            Long targetUserId = args.asLong(1);
            log.info("action=user.block result=skipped_duplicate blockerId={} blockedId={}", blockerId, targetUserId);
        });
        handlers.put(UserActionAuditType.USER_UNBLOCK_SKIPPED_NOT_FOUND, args -> {
            Long blockerId = args.asLong(0);
            Long targetUserId = args.asLong(1);
            log.info("action=user.unblock result=skipped_not_found blockerId={} blockedId={}", blockerId, targetUserId);
        });
        handlers.put(UserActionAuditType.USER_BLOCK_FAILED_UNAUTHENTICATED, args -> {
            Long targetUserId = args.asLong(0);
            log.warn("action=user.block result=failed reason=unauthenticated targetUserId={}", targetUserId);
        });
        handlers.put(UserActionAuditType.USER_BLOCK_FAILED,
                args -> logWarnWithOptionalThrowable("action=user.block result=failed blockerId={} blockedId={} reason={}", args));
        handlers.put(UserActionAuditType.USER_UNBLOCK_FAILED_UNAUTHENTICATED, args -> {
            Long targetUserId = args.asLong(0);
            log.warn("action=user.unblock result=failed reason=unauthenticated targetUserId={}", targetUserId);
        });
        handlers.put(UserActionAuditType.USER_UNBLOCK_FAILED,
                args -> logWarnWithOptionalThrowable("action=user.unblock result=failed blockerId={} blockedId={} reason={}", args));

        return handlers;
    }

    private static void recordStateChange(
            UserActionAuditArgs args,
            UserActionAuditRecorder recorder,
            String logMessage,
            String activityType) {
        Long blockerId = args.asLong(0);
        Long targetUserId = args.asLong(1);
        log.info(logMessage, blockerId, targetUserId);
        recorder.record(blockerId, activityType, "targetUserId=" + args.safeId(targetUserId));
    }

    private static void logWarnWithOptionalThrowable(String message, UserActionAuditArgs args) {
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
