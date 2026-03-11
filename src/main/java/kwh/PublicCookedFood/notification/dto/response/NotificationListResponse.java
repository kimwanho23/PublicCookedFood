package kwh.PublicCookedFood.notification.dto.response;

import kwh.PublicCookedFood.common.util.ImmutableCollections;
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
        notifications = ImmutableCollections.immutableList(notifications);
    }

    @Override
    public List<NotificationResponse> notifications() {
        return notifications;
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
