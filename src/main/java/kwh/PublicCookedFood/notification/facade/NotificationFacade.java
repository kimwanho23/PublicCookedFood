package kwh.PublicCookedFood.notification.facade;

import kwh.PublicCookedFood.notification.dto.response.NotificationDeleteAllResponse;
import kwh.PublicCookedFood.notification.dto.response.NotificationListResponse;
import kwh.PublicCookedFood.notification.dto.response.NotificationReadAllResponse;
import kwh.PublicCookedFood.notification.dto.response.NotificationResponse;
import kwh.PublicCookedFood.notification.dto.response.NotificationSettingResponse;
import kwh.PublicCookedFood.notification.service.NotificationService;
import kwh.PublicCookedFood.user.domain.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class NotificationFacade {

    private static final String LOGIN_REQUIRED_MESSAGE = "로그인이 필요합니다.";

    private final NotificationService notificationService;

    public SseEmitter subscribe(Users user) {
        return notificationService.subscribe(requireUserId(user));
    }

    public NotificationListResponse getNotifications(Users user,
                                                     Pageable pageable,
                                                     boolean unreadOnly) {
        Long userId = requireUserId(user);
        Page<NotificationResponse> notifications = notificationService.getNotifications(userId, pageable, unreadOnly);
        long unreadCount = notificationService.getUnreadCount(userId);
        return NotificationListResponse.from(notifications, unreadCount);
    }

    public NotificationSettingResponse getNotificationSetting(Users user) {
        boolean enabled = notificationService.isNotificationEnabled(requireUserId(user));
        return new NotificationSettingResponse(enabled);
    }

    public NotificationSettingResponse updateNotificationSetting(Users user, boolean enabled) {
        boolean updated = notificationService.updateNotificationEnabled(requireUserId(user), enabled);
        return new NotificationSettingResponse(updated);
    }

    public void markAsRead(Users user, Long notificationId) {
        notificationService.markAsRead(requireUserId(user), notificationId);
    }

    public NotificationReadAllResponse markAllAsRead(Users user) {
        int updatedCount = notificationService.markAllAsRead(requireUserId(user));
        return new NotificationReadAllResponse(updatedCount);
    }

    public void deleteNotification(Users user, Long notificationId) {
        notificationService.deleteNotification(requireUserId(user), notificationId);
    }

    public NotificationDeleteAllResponse deleteAllNotifications(Users user, boolean unreadOnly) {
        long deletedCount = notificationService.deleteAllNotifications(requireUserId(user), unreadOnly);
        return new NotificationDeleteAllResponse(deletedCount);
    }

    private Long requireUserId(Users user) {
        if (user == null || user.getId() == null) {
            throw new IllegalStateException(LOGIN_REQUIRED_MESSAGE);
        }
        return user.getId();
    }
}
