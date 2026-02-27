package kwh.PublicCookedFood.user.audit;

import kwh.PublicCookedFood.user.aop.UserActionAuditType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationAuditPublisher {

    private final AuditEventDispatcher dispatcher;

    public void notificationSettingUpdate(Long userId, Boolean enabled) {
        dispatcher.publishUserAction(UserActionAuditType.NOTIFICATION_SETTING_UPDATE, userId, enabled);
    }
}
