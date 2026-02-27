package kwh.PublicCookedFood.notification.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import kwh.PublicCookedFood.config.oauth2.LoginUser;
import kwh.PublicCookedFood.notification.dto.response.NotificationDeleteAllResponse;
import kwh.PublicCookedFood.notification.dto.response.NotificationListResponse;
import kwh.PublicCookedFood.notification.dto.response.NotificationReadAllResponse;
import kwh.PublicCookedFood.notification.dto.response.NotificationSettingResponse;
import kwh.PublicCookedFood.notification.facade.NotificationFacade;
import kwh.PublicCookedFood.user.domain.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/notifications")
@Tag(name = "Notification API")
public class NotificationController {

    private final NotificationFacade notificationFacade;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@LoginUser Users user) {
        return notificationFacade.subscribe(user);
    }

    @GetMapping("")
    public ResponseEntity<NotificationListResponse> getNotifications(
            @LoginUser Users user,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @PageableDefault(page = 0, size = 20, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(notificationFacade.getNotifications(user, pageable, unreadOnly));
    }

    @GetMapping("/setting")
    public ResponseEntity<NotificationSettingResponse> getNotificationSetting(@LoginUser Users user) {
        return ResponseEntity.ok(notificationFacade.getNotificationSetting(user));
    }

    @PatchMapping("/setting")
    public ResponseEntity<NotificationSettingResponse> updateNotificationSetting(@LoginUser Users user,
                                                                                 @RequestParam boolean enabled) {
        return ResponseEntity.ok(notificationFacade.updateNotificationSetting(user, enabled));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@LoginUser Users user, @PathVariable @Positive Long notificationId) {
        notificationFacade.markAsRead(user, notificationId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<NotificationReadAllResponse> markAllAsRead(@LoginUser Users user) {
        return ResponseEntity.ok(notificationFacade.markAllAsRead(user));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@LoginUser Users user, @PathVariable @Positive Long notificationId) {
        notificationFacade.deleteNotification(user, notificationId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("")
    public ResponseEntity<NotificationDeleteAllResponse> deleteAllNotifications(
            @LoginUser Users user,
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        return ResponseEntity.ok(notificationFacade.deleteAllNotifications(user, unreadOnly));
    }
}
