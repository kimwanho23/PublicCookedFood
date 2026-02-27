package kwh.PublicCookedFood.user.aop.action;

import kwh.PublicCookedFood.user.aop.UserActionAuditType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class UserProfileAuditStrategy extends AbstractUserActionAuditStrategy {

    public UserProfileAuditStrategy(UserActionAuditRecorder recorder) {
        super(createHandlers(recorder));
    }

    private static Map<UserActionAuditType, Consumer<UserActionAuditArgs>> createHandlers(
            UserActionAuditRecorder recorder) {
        Map<UserActionAuditType, Consumer<UserActionAuditArgs>> handlers = newHandlers();

        handlers.put(UserActionAuditType.USER_SIGNUP, args -> {
            Long userId = args.asLong(0);
            String email = args.asString(1);
            log.info("action=user.signup result=success userId={} email={}", userId, email);
            recorder.record(userId, "USER_SIGNUP", "email=" + args.safeText(email));
        });
        handlers.put(UserActionAuditType.USER_PROFILE_UPDATE, args -> {
            Long userId = args.asLong(0);
            log.info("action=user.profile_update result=success userId={}", userId);
            recorder.record(userId, "USER_PROFILE_UPDATE", null);
        });
        handlers.put(UserActionAuditType.USER_PROFILE_UPDATE_FAILED_UNAUTHENTICATED,
                args -> log.warn("action=user.profile_update result=failed reason=unauthenticated"));
        handlers.put(UserActionAuditType.USER_PROFILE_UPDATE_FAILED_VALIDATION, args -> {
            Long userId = args.asLong(0);
            log.warn("action=user.profile_update result=failed reason=validation_error userId={}", userId);
        });
        handlers.put(UserActionAuditType.USER_PROFILE_UPDATE_FAILED_USER_NOT_FOUND, args -> {
            Long userId = args.asLong(0);
            log.warn("action=user.profile_update result=failed reason=user_not_found userId={}", userId);
        });
        handlers.put(UserActionAuditType.USER_PROFILE_UPDATE_FAILED, args -> {
            Long userId = args.asLong(0);
            String reason = args.asString(1);
            log.warn("action=user.profile_update result=failed userId={} reason={}", userId, reason);
        });

        return handlers;
    }
}
