package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.notification.domain.Notification;
import kwh.PublicCookedFood.user.domain.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class NewCommentNotificationDispatchStrategy implements NotificationDispatchStrategy {

    private final NotificationDispatchSupport support;

    @Override
    public NotificationDispatchType type() {
        return NotificationDispatchType.NEW_COMMENT;
    }

    @Override
    public void dispatch(NotificationDispatchContext context) {
        Comments comment = context.comment();
        if (comment == null || comment.getBoard() == null || comment.getUser() == null) {
            return;
        }

        Users actor = comment.getUser();
        Board board = comment.getBoard();
        Users boardOwner = board.getUser();
        Comments parent = comment.getParent();
        Users parentCommentOwner = parent == null ? null : parent.getUser();
        String preview = support.buildPreview(comment.getContents());
        Set<Long> notifiedReceiverIds = new LinkedHashSet<>();

        if (!support.isSameUser(parentCommentOwner, actor)) {
            createReplyNotificationIfNeeded(parentCommentOwner, actor, board, comment, preview, notifiedReceiverIds);
        }

        if (boardOwner != null
                && !support.isSameUser(boardOwner, actor)
                && !support.isSameUser(boardOwner, parentCommentOwner)
                && support.canReceiveNotification(boardOwner, actor)) {
            Notification saved = support.saveAndPublish(
                    Notification.boardComment(boardOwner, actor, board, comment, preview)
            );
            if (saved.getReceiver() != null && saved.getReceiver().getId() != null) {
                notifiedReceiverIds.add(saved.getReceiver().getId());
            }
        }

        for (Users mentionedUser : support.resolveMentionedUsers(comment.getContents(), actor.getId())) {
            if (mentionedUser.getId() == null || notifiedReceiverIds.contains(mentionedUser.getId())) {
                continue;
            }
            if (!support.canReceiveNotification(mentionedUser, actor)) {
                continue;
            }
            Notification saved = support.saveAndPublish(
                    Notification.commentMention(mentionedUser, actor, board, comment, preview)
            );
            if (saved.getReceiver() != null && saved.getReceiver().getId() != null) {
                notifiedReceiverIds.add(saved.getReceiver().getId());
            }
        }
    }

    private void createReplyNotificationIfNeeded(Users receiver,
                                                 Users actor,
                                                 Board board,
                                                 Comments comment,
                                                 String preview,
                                                 Set<Long> notifiedReceiverIds) {
        if (receiver == null || !support.canReceiveNotification(receiver, actor)) {
            return;
        }
        Notification saved = support.saveAndPublish(
                Notification.commentReply(receiver, actor, board, comment, preview)
        );
        if (saved.getReceiver() != null && saved.getReceiver().getId() != null) {
            notifiedReceiverIds.add(saved.getReceiver().getId());
        }
    }
}

