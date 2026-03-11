package kwh.PublicCookedFood.notification.facade;

import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.notification.dto.response.NotificationInitialStateResponse;
import kwh.PublicCookedFood.notification.dto.response.NotificationListResponse;
import kwh.PublicCookedFood.notification.dto.response.NotificationResponse;
import kwh.PublicCookedFood.notification.dto.response.NotificationSettingResponse;
import kwh.PublicCookedFood.notification.error.NotificationErrorCode;
import kwh.PublicCookedFood.notification.service.NotificationService;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.common.error.CommonErrorCode;
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
        NotificationAccountSession currentAccount = NotificationAccountSession.from(account);
        if (!currentAccount.notificationEnabled()) {
            throw new AppException(NotificationErrorCode.NOTIFICATION_DISABLED);
        }
        return notificationService.subscribe(currentAccount.accountId());
    }

    public NotificationInitialStateResponse getInitialState(Account account,
                                                            Pageable pageable,
                                                            boolean unreadOnly) {
        NotificationAccountSession currentAccount = NotificationAccountSession.from(account);
        Page<NotificationResponse> notifications =
                notificationService.getNotifications(currentAccount.accountId(), pageable, unreadOnly);
        long unreadCount = notificationService.getUnreadCount(currentAccount.accountId());
        return NotificationInitialStateResponse.from(
                currentAccount.notificationEnabled(),
                notifications,
                unreadCount
        );
    }

    public NotificationListResponse getNotifications(Account account,
                                                     Pageable pageable,
                                                     boolean unreadOnly) {
        NotificationAccountSession currentAccount = NotificationAccountSession.from(account);
        Page<NotificationResponse> notifications =
                notificationService.getNotifications(currentAccount.accountId(), pageable, unreadOnly);
        long unreadCount = notificationService.getUnreadCount(currentAccount.accountId());
        return NotificationListResponse.from(notifications, unreadCount);
    }

    public NotificationSettingResponse getNotificationSetting(Account account) {
        NotificationAccountSession currentAccount = NotificationAccountSession.from(account);
        boolean enabled = notificationService.isNotificationEnabled(currentAccount.accountId());
        return new NotificationSettingResponse(enabled);
    }

    public NotificationSettingResponse updateNotificationSetting(Account account, boolean enabled) {
        NotificationAccountSession currentAccount = NotificationAccountSession.from(account);
        boolean updated = notificationService.updateNotificationEnabled(currentAccount.accountId(), enabled);
        currentAccount.updateNotificationEnabled(updated);
        return new NotificationSettingResponse(updated);
    }

    public void markAsRead(Account account, Long notificationId) {
        NotificationAccountSession currentAccount = NotificationAccountSession.from(account);
        notificationService.markAsRead(currentAccount.accountId(), notificationId);
    }

    public void markAllAsRead(Account account) {
        NotificationAccountSession currentAccount = NotificationAccountSession.from(account);
        notificationService.markAllAsRead(currentAccount.accountId());
    }

    public void deleteNotification(Account account, Long notificationId) {
        NotificationAccountSession currentAccount = NotificationAccountSession.from(account);
        notificationService.deleteNotification(currentAccount.accountId(), notificationId);
    }

    public void deleteAllNotifications(Account account, boolean unreadOnly) {
        NotificationAccountSession currentAccount = NotificationAccountSession.from(account);
        notificationService.deleteAllNotifications(currentAccount.accountId(), unreadOnly);
    }

    private record NotificationAccountSession(Account account) {

        private static NotificationAccountSession from(Account account) {
            if (account == null || account.getId() == null) {
                throw new AppException(CommonErrorCode.AUTHENTICATION_REQUIRED);
            }
            return new NotificationAccountSession(account);
        }

        private long accountId() {
            return account.getId();
        }

        private boolean notificationEnabled() {
            return account.isNotificationEnabled();
        }

        private void updateNotificationEnabled(boolean enabled) {
            account.updateNotificationEnabled(enabled);
        }
    }
}
