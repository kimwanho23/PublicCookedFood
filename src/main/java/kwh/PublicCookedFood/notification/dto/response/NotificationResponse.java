package kwh.PublicCookedFood.notification.dto.response;

import kwh.PublicCookedFood.notification.domain.Notification;
import kwh.PublicCookedFood.notification.domain.NotificationType;

import java.time.LocalDateTime;
import java.util.Objects;

public record NotificationResponse(
        Long id,
        NotificationType type,
        Long actorId,
        String actorName,
        Long boardId,
        Long commentId,
        String contentPreview,
        boolean isRead,
        LocalDateTime readTime,
        LocalDateTime regTime,
        String targetPath
) {
    public static NotificationResponse from(Notification notification) {
        Notification safeNotification = Objects.requireNonNull(notification, "notification");
        String targetPath = "/boards";
        if (safeNotification.getBoard() != null && safeNotification.getBoard().getId() != null) {
            targetPath = "/boards/" + safeNotification.getBoard().getId();
            if (safeNotification.getComment() != null && safeNotification.getComment().getId() != null) {
                targetPath = targetPath + "#comment-" + safeNotification.getComment().getId();
            }
        }
        return from(safeNotification, targetPath);
    }

    public static NotificationResponse from(Notification notification, String targetPath) {
        Notification safeNotification = Objects.requireNonNull(notification, "notification");
        return new NotificationResponse(
                safeNotification.getId(),
                safeNotification.getType(),
                safeNotification.getActor().getId(),
                safeNotification.getActor().getName(),
                safeNotification.getBoard().getId(),
                safeNotification.getComment() == null ? null : safeNotification.getComment().getId(),
                safeNotification.getContentPreview(),
                safeNotification.isRead(),
                safeNotification.getReadTime(),
                safeNotification.getRegTime(),
                targetPath
        );
    }
}
