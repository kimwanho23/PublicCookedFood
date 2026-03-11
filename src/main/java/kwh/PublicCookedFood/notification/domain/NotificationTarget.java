package kwh.PublicCookedFood.notification.domain;

public sealed interface NotificationTarget permits BoardNotificationTarget, CommentNotificationTarget {

    long boardId();
}
