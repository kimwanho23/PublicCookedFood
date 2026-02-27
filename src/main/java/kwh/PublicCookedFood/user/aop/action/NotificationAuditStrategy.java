package kwh.PublicCookedFood.user.aop.action;

import kwh.PublicCookedFood.user.aop.UserActionAuditType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class NotificationAuditStrategy extends AbstractUserActionAuditStrategy {

    public NotificationAuditStrategy(UserActionAuditRecorder recorder) {
        super(createHandlers(recorder));
    }

    private static Map<UserActionAuditType, Consumer<UserActionAuditArgs>> createHandlers(
            UserActionAuditRecorder recorder) {
        Map<UserActionAuditType, Consumer<UserActionAuditArgs>> handlers = newHandlers();

        handlers.put(UserActionAuditType.NOTIFICATION_SETTING_UPDATE, args -> {
            Long userId = args.asLong(0);
            Boolean enabled = args.asBoolean(1);
            log.info("action=notification.setting_update result=success userId={} enabled={}", userId, enabled);
            recorder.record(userId, "NOTIFICATION_SETTING_UPDATE", "enabled=" + args.safeBoolean(enabled));
        });

        return handlers;
    }
}
