package kwh.PublicCookedFood.notification.domain;

public record CommentNotificationTarget(long boardId, long commentId) implements NotificationTarget {
}
