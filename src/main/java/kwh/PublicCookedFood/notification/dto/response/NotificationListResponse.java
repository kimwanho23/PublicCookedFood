package kwh.PublicCookedFood.notification.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record NotificationListResponse(
        List<NotificationResponse> notifications,
        long unreadCount,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public NotificationListResponse {
        notifications = notifications == null ? List.of() : List.copyOf(notifications);
    }

    @Override
    public List<NotificationResponse> notifications() {
        return List.copyOf(notifications);
    }

    public static NotificationListResponse from(Page<NotificationResponse> pageResult, long unreadCount) {
        return new NotificationListResponse(
                pageResult.getContent(),
                unreadCount,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }
}
