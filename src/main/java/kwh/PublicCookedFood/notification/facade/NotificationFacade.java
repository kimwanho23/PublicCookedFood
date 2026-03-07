package kwh.PublicCookedFood.notification.facade;

import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.CommonErrorCode;
import kwh.PublicCookedFood.notification.dto.response.NotificationListResponse;
import kwh.PublicCookedFood.notification.dto.response.NotificationResponse;
import kwh.PublicCookedFood.notification.dto.response.NotificationSettingResponse;
import kwh.PublicCookedFood.notification.error.NotificationErrorCode;
import kwh.PublicCookedFood.notification.service.NotificationService;
import kwh.PublicCookedFood.account.domain.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class NotificationFacade {

    private final NotificationService notificationService;

    public SseEmitter subscribe(Account account) {
        Long accountId = requireAccountId(account);
        if (!notificationService.isNotificationEnabled(accountId)) {
            throw new AppException(NotificationErrorCode.NOTIFICATION_DISABLED);
        }
        return notificationService.subscribe(accountId);
    }

    public NotificationListResponse getNotifications(Account account,
                                                     Pageable pageable,
                                                     boolean unreadOnly) {
        Long accountId = requireAccountId(account);
        Page<NotificationResponse> notifications = notificationService.getNotifications(accountId, pageable, unreadOnly);
        long unreadCount = notificationService.getUnreadCount(accountId);
        return NotificationListResponse.from(notifications, unreadCount);
    }

    public NotificationSettingResponse getNotificationSetting(Account account) {
        boolean enabled = notificationService.isNotificationEnabled(requireAccountId(account));
        return new NotificationSettingResponse(enabled);
    }

    public NotificationSettingResponse updateNotificationSetting(Account account, boolean enabled) {
        boolean updated = notificationService.updateNotificationEnabled(requireAccountId(account), enabled);
        account.updateNotificationEnabled(updated);
        return new NotificationSettingResponse(updated);
    }

    public void markAsRead(Account account, Long notificationId) {
        notificationService.markAsRead(requireAccountId(account), notificationId);
    }

    public void markAllAsRead(Account account) {
        notificationService.markAllAsRead(requireAccountId(account));
    }

    public void deleteNotification(Account account, Long notificationId) {
        notificationService.deleteNotification(requireAccountId(account), notificationId);
    }

    public void deleteAllNotifications(Account account, boolean unreadOnly) {
        notificationService.deleteAllNotifications(requireAccountId(account), unreadOnly);
    }

    private Long requireAccountId(Account account) {
        return requireAccount(account).getId();
    }

    private Account requireAccount(Account account) {
        if (account == null || account.getId() == null) {
            throw new AppException(CommonErrorCode.AUTHENTICATION_REQUIRED);
        }
        return account;
    }
}
