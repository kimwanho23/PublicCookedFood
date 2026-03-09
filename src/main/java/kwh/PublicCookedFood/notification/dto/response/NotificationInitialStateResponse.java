package kwh.PublicCookedFood.notification.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record NotificationInitialStateResponse(
        boolean enabled,
        List<NotificationResponse> notifications,
        long unreadCount,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public NotificationInitialStateResponse {
        notifications = notifications == null ? List.of() : List.copyOf(notifications);
    }

    @Override
    public List<NotificationResponse> notifications() {
        return List.copyOf(notifications);
    }

    public static NotificationInitialStateResponse from(boolean enabled,
                                                        Page<NotificationResponse> pageResult,
                                                        long unreadCount) {
        return new NotificationInitialStateResponse(
                enabled,
                pageResult.getContent(),
                unreadCount,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }
}
