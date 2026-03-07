package kwh.PublicCookedFood.notification.dto.response;

import kwh.PublicCookedFood.notification.domain.Notification;
import kwh.PublicCookedFood.notification.domain.NotificationType;

import java.time.LocalDateTime;

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
        String targetPath = "/boards";
        if (notification != null && notification.getBoard() != null && notification.getBoard().getId() != null) {
            targetPath = "/boards/" + notification.getBoard().getId();
            if (notification.getComment() != null && notification.getComment().getId() != null) {
                targetPath = targetPath + "#comment-" + notification.getComment().getId();
            }
        }
        return from(notification, targetPath);
    }

    public static NotificationResponse from(Notification notification, String targetPath) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getActor().getId(),
                notification.getActor().getName(),
                notification.getBoard().getId(),
                notification.getComment() == null ? null : notification.getComment().getId(),
                notification.getContentPreview(),
                notification.isRead(),
                notification.getReadTime(),
                notification.getRegTime(),
                targetPath
        );
    }
}
