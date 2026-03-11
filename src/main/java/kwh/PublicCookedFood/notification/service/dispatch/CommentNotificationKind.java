package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.notification.domain.Notification;

enum CommentNotificationKind {
    REPLY(3),
    BOARD_COMMENT(2),
    MENTION(1);

    private final int priority;

    CommentNotificationKind(int priority) {
        this.priority = priority;
    }

    Notification create(CommentNotificationContext context, Account receiver) {
        return switch (this) {
            case REPLY -> Notification.commentReply(
                    receiver,
                    context.actor(),
                    context.board(),
                    context.comment(),
                    context.preview()
            );
            case BOARD_COMMENT -> Notification.boardComment(
                    receiver,
                    context.actor(),
                    context.board(),
                    context.comment(),
                    context.preview()
            );
            case MENTION -> Notification.commentMention(
                    receiver,
                    context.actor(),
                    context.board(),
                    context.comment(),
                    context.preview()
            );
        };
    }

    boolean higherThan(CommentNotificationKind other) {
        return priority > other.priority;
    }
}
