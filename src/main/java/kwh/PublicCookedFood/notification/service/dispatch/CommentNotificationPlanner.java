package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.account.domain.Account;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;

@Component
class CommentNotificationPlanner {

    CommentNotificationPlan plan(CommentNotificationContext context) {
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

        private final LinkedHashMap<Long, CommentNotificationPlan.PlannedCommentNotification> notificationsByReceiver =
                new LinkedHashMap<>();

        private void register(CommentNotificationKind kind, Account receiver) {
            long receiverId = receiver.getId();
            CommentNotificationPlan.PlannedCommentNotification existing = notificationsByReceiver.get(receiverId);
            if (existing != null && !kind.higherThan(existing.kind())) {
                return;
            }
            notificationsByReceiver.put(receiverId, new CommentNotificationPlan.PlannedCommentNotification(kind, receiver));
        }

        private CommentNotificationPlan toPlan() {
            return new CommentNotificationPlan(new ArrayList<>(notificationsByReceiver.values()));
        }
    }
}
