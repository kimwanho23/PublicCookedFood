package kwh.PublicCookedFood.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import kwh.PublicCookedFood.config.oauth2.LoginAccount;
import kwh.PublicCookedFood.notification.dto.response.NotificationInitialStateResponse;
import kwh.PublicCookedFood.notification.dto.response.NotificationListResponse;
import kwh.PublicCookedFood.notification.dto.response.NotificationSettingResponse;
import kwh.PublicCookedFood.notification.facade.NotificationFacade;
import kwh.PublicCookedFood.account.domain.Account;
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
import org.springdoc.core.annotations.ParameterObject;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/notifications")
@Tag(name = "알림 API", description = "실시간 알림 구독과 알림 목록 관리 기능을 제공하는 API")
public class NotificationController {

    private final NotificationFacade notificationFacade;

    @Operation(summary = "실시간 알림 구독", description = "SSE 연결을 열어 현재 로그인 사용자의 실시간 알림을 구독합니다.")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@Parameter(hidden = true) @LoginAccount Account account) {
        return notificationFacade.subscribe(account);
    }

    @Operation(summary = "알림 초기 상태 조회", description = "헤더 초기 렌더에 필요한 현재 알림 상태를 조회합니다.")
    @GetMapping("/initial-state")
    public ResponseEntity<NotificationInitialStateResponse> getInitialState(
            @Parameter(hidden = true) @LoginAccount Account account,
            @Parameter(description = "true이면 읽지 않은 알림만 조회합니다.")
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @ParameterObject
            @PageableDefault(page = 0, size = 8, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(notificationFacade.getInitialState(account, pageable, unreadOnly));
    }

    @Operation(summary = "알림 목록 조회", description = "현재 로그인 사용자의 알림 목록을 페이징하여 조회합니다.")
    @GetMapping("")
    public ResponseEntity<NotificationListResponse> getNotifications(
            @Parameter(hidden = true) @LoginAccount Account account,
            @Parameter(description = "true이면 읽지 않은 알림만 조회합니다.")
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @ParameterObject
            @PageableDefault(page = 0, size = 20, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(notificationFacade.getNotifications(account, pageable, unreadOnly));
    }

    @Operation(summary = "알림 설정 조회", description = "현재 로그인 사용자의 알림 수신 설정을 조회합니다.")
    @GetMapping("/setting")
    public ResponseEntity<NotificationSettingResponse> getNotificationSetting(
            @Parameter(hidden = true) @LoginAccount Account account
    ) {
        return ResponseEntity.ok(notificationFacade.getNotificationSetting(account));
    }

    @Operation(summary = "알림 설정 변경", description = "현재 로그인 사용자의 알림 수신 여부를 변경합니다.")
    @PatchMapping("/setting")
    public ResponseEntity<NotificationSettingResponse> updateNotificationSetting(
            @Parameter(hidden = true) @LoginAccount Account account,
            @Parameter(description = "알림 수신 활성화 여부", required = true)
            @RequestParam boolean enabled
    ) {
        return ResponseEntity.ok(notificationFacade.updateNotificationSetting(account, enabled));
    }

    @Operation(summary = "알림 단건 읽음 처리", description = "특정 알림 1건을 읽음 상태로 변경합니다.")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @Parameter(hidden = true) @LoginAccount Account account,
            @Parameter(description = "읽음 처리할 알림 ID", required = true)
            @PathVariable @Positive Long notificationId
    ) {
        notificationFacade.markAsRead(account, notificationId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "알림 전체 읽음 처리", description = "현재 로그인 사용자의 모든 알림을 읽음 상태로 변경합니다.")
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@Parameter(hidden = true) @LoginAccount Account account) {
        notificationFacade.markAllAsRead(account);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "알림 단건 삭제", description = "특정 알림 1건을 삭제합니다.")
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @Parameter(hidden = true) @LoginAccount Account account,
            @Parameter(description = "삭제할 알림 ID", required = true)
            @PathVariable @Positive Long notificationId
    ) {
        notificationFacade.deleteNotification(account, notificationId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "알림 전체 삭제", description = "조건에 맞는 현재 로그인 사용자의 알림을 전체 삭제합니다.")
    @DeleteMapping("")
    public ResponseEntity<Void> deleteAllNotifications(
            @Parameter(hidden = true) @LoginAccount Account account,
            @Parameter(description = "true이면 읽지 않은 알림만 삭제합니다.")
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        notificationFacade.deleteAllNotifications(account, unreadOnly);
        return ResponseEntity.noContent().build();
    }
}
