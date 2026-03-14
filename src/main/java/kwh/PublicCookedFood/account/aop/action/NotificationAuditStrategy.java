package kwh.PublicCookedFood.account.aop.action;

import kwh.PublicCookedFood.account.aop.AccountActionAuditType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class NotificationAuditStrategy extends AbstractAccountActionAuditStrategy {

    public NotificationAuditStrategy(AccountActionAuditRecorder recorder) {
        super(createHandlers(recorder));
    }

    private static Map<AccountActionAuditType, Consumer<AccountActionAuditArgs>> createHandlers(
            AccountActionAuditRecorder recorder) {
        Map<AccountActionAuditType, Consumer<AccountActionAuditArgs>> handlers = newHandlers();

        handlers.put(AccountActionAuditType.NOTIFICATION_SETTING_UPDATE, args -> {
            Long accountId = args.asLong(0);
            Boolean enabled = args.asBoolean(1);
            log.info("action=notification.setting_update result=success accountId={} enabled={}",
                    args.displayId(accountId), args.displayBoolean(enabled));
            recorder.record(accountId, "NOTIFICATION_SETTING_UPDATE", "enabled=" + args.displayBoolean(enabled));
        });

        return handlers;
    }
}

