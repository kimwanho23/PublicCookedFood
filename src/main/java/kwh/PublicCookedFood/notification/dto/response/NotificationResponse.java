package kwh.PublicCookedFood.notification.dto.response;

import kwh.PublicCookedFood.notification.domain.CommentNotificationTarget;
import kwh.PublicCookedFood.notification.domain.Notification;
import kwh.PublicCookedFood.notification.domain.NotificationTarget;
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
    public static NotificationResponse from(Notification notification, String targetPath) {
        NotificationTarget target = notification.target();
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getActor().getId(),
                notification.getActor().getName(),
                target.boardId(),
                target instanceof CommentNotificationTarget commentTarget ? commentTarget.commentId() : null,
                notification.getContentPreview(),
                notification.isRead(),
                notification.getReadTime(),
                notification.getRegTime(),
                targetPath
        );
    }
}
