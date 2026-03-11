package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.common.util.ImmutableCollections;
import kwh.PublicCookedFood.notification.domain.Notification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Component
class CommentNotificationPlanner {

    Plan plan(CommentNotificationContext context) {
        ReceiverNotifications notifications = new ReceiverNotifications();
        registerReplyNotification(context, notifications);
        registerBoardCommentNotification(context, notifications);
        registerMentionNotifications(context, notifications);
        return notifications.toPlan();
    }

    private void registerReplyNotification(CommentNotificationContext context,
                                           ReceiverNotifications notifications) {
        if (!context.hasReplyOwner()) {
            return;
        }
        Account replyOwner = context.replyOwner();
        if (context.isActor(replyOwner) || !context.canNotify(replyOwner)) {
            return;
        }
        notifications.register(CommentNotificationKind.REPLY, replyOwner);
    }

    private void registerBoardCommentNotification(CommentNotificationContext context,
                                                  ReceiverNotifications notifications) {
        Account boardOwner = context.boardOwner();
        if (context.isActor(boardOwner) || context.isBoardOwnerAlsoReplyOwner() || !context.canNotify(boardOwner)) {
            return;
        }
        notifications.register(CommentNotificationKind.BOARD_COMMENT, boardOwner);
    }

    private void registerMentionNotifications(CommentNotificationContext context,
                                              ReceiverNotifications notifications) {
        for (Account mentionedUser : context.mentionedUsers()) {
            if (context.canNotify(mentionedUser)) {
                notifications.register(CommentNotificationKind.MENTION, mentionedUser);
            }
        }
    }

    private static final class ReceiverNotifications {

        private final LinkedHashMap<Long, PlannedCommentNotification> notificationsByReceiver =
                new LinkedHashMap<>();

        private void register(CommentNotificationKind kind, Account receiver) {
            long receiverId = receiver.getId();
            PlannedCommentNotification existing = notificationsByReceiver.get(receiverId);
            if (existing != null && !kind.higherThan(existing.kind())) {
                return;
            }
            notificationsByReceiver.put(receiverId, new PlannedCommentNotification(kind, receiver));
        }

        private Plan toPlan() {
            return new Plan(new ArrayList<>(notificationsByReceiver.values()));
        }
    }

    static final class Plan {

        private final List<PlannedCommentNotification> notifications;

        private Plan(List<PlannedCommentNotification> notifications) {
            this.notifications = ImmutableCollections.immutableList(notifications);
        }

        List<Notification> toNotifications(CommentNotificationContext context) {
            List<Notification> resolvedNotifications = new ArrayList<>(notifications.size());
            for (PlannedCommentNotification notification : notifications) {
                resolvedNotifications.add(notification.toNotification(context));
            }
            return resolvedNotifications;
        }
    }

    record PlannedCommentNotification(CommentNotificationKind kind, Account receiver) {

        private Notification toNotification(CommentNotificationContext context) {
            return kind.create(context, receiver);
        }
    }
}
