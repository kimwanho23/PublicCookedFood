package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.common.util.ImmutableCollections;
import kwh.PublicCookedFood.notification.domain.Notification;

import java.util.ArrayList;
import java.util.List;

final class CommentNotificationPlan {

    private final List<PlannedCommentNotification> notifications;

    CommentNotificationPlan(List<PlannedCommentNotification> notifications) {
        this.notifications = ImmutableCollections.immutableList(notifications);
    }

    List<Notification> toNotifications(CommentNotificationContext context) {
        List<Notification> resolvedNotifications = new ArrayList<>(notifications.size());
        for (PlannedCommentNotification notification : notifications) {
            resolvedNotifications.add(notification.toNotification(context));
        }
        return resolvedNotifications;
    }

    record PlannedCommentNotification(CommentNotificationKind kind, Account receiver) {

        private Notification toNotification(CommentNotificationContext context) {
            return kind.create(context, receiver);
        }
    }
}
