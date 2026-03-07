package kwh.PublicCookedFood.account.audit;

import kwh.PublicCookedFood.account.aop.AccountActionAuditType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationAuditPublisher {

    private final AuditEventDispatcher dispatcher;

    public void notificationSettingUpdate(Long accountId, Boolean enabled) {
        dispatcher.publishAccountAction(AccountActionAuditType.NOTIFICATION_SETTING_UPDATE, accountId, enabled);
    }
}

