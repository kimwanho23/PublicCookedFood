package kwh.PublicCookedFood.notification.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import kwh.PublicCookedFood.config.oauth2.LoginUser;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/notifications")
@Tag(name = "Notification API")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@LoginUser Users user) {
        if (user == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        return notificationService.subscribe(user.getId());
    }

    @GetMapping("")
    public ResponseEntity<NotificationListResponse> getNotifications(
            @LoginUser Users user,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @PageableDefault(page = 0, size = 20, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (user == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        Page<NotificationResponse> notifications = notificationService.getNotifications(user.getId(), pageable, unreadOnly);
        long unreadCount = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(NotificationListResponse.from(notifications, unreadCount));
    }

    @GetMapping("/setting")
    public ResponseEntity<NotificationSettingResponse> getNotificationSetting(@LoginUser Users user) {
        if (user == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        boolean enabled = notificationService.isNotificationEnabled(user.getId());
        return ResponseEntity.ok(new NotificationSettingResponse(enabled));
    }

    @PatchMapping("/setting")
    public ResponseEntity<NotificationSettingResponse> updateNotificationSetting(@LoginUser Users user,
                                                                                 @RequestParam boolean enabled) {
        if (user == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        boolean updated = notificationService.updateNotificationEnabled(user.getId(), enabled);
        return ResponseEntity.ok(new NotificationSettingResponse(updated));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@LoginUser Users user, @PathVariable @Positive Long notificationId) {
        if (user == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        notificationService.markAsRead(user.getId(), notificationId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<NotificationReadAllResponse> markAllAsRead(@LoginUser Users user) {
        if (user == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        int updatedCount = notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(new NotificationReadAllResponse(updatedCount));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@LoginUser Users user, @PathVariable @Positive Long notificationId) {
        if (user == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        notificationService.deleteNotification(user.getId(), notificationId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("")
    public ResponseEntity<NotificationDeleteAllResponse> deleteAllNotifications(
            @LoginUser Users user,
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        if (user == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        long deletedCount = notificationService.deleteAllNotifications(user.getId(), unreadOnly);
        return ResponseEntity.ok(new NotificationDeleteAllResponse(deletedCount));
    }
}
