package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.notification.domain.Notification;
import kwh.PublicCookedFood.account.domain.Account;
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
        if (comment == null || comment.getBoard() == null || comment.getAccount() == null) {
            return;
        }

        Account actor = comment.getAccount();
        Board board = comment.getBoard();
        Account boardOwner = board.getAccount();
        Comments parent = comment.getParent();
        Account parentCommentOwner = parent == null ? null : parent.getAccount();
        String preview = support.buildPreview(comment.getContents());
        Set<Long> notifiedReceiverIds = new LinkedHashSet<>();
        Set<Account> mentionedUsers = support.resolveMentionedUsers(comment.getContents(), actor.getId());
        Set<Account> candidateReceivers = new LinkedHashSet<>();

        if (!support.isSameAccount(parentCommentOwner, actor) && parentCommentOwner != null) {
            candidateReceivers.add(parentCommentOwner);
        }
        if (boardOwner != null
                && !support.isSameAccount(boardOwner, actor)
                && !support.isSameAccount(boardOwner, parentCommentOwner)) {
            candidateReceivers.add(boardOwner);
        }
        candidateReceivers.addAll(mentionedUsers);
        Set<Long> restrictedReceiverIds = support.resolveRestrictedReceiverIds(actor, candidateReceivers);

        if (!support.isSameAccount(parentCommentOwner, actor)) {
            createReplyNotificationIfNeeded(parentCommentOwner, board, comment, preview, notifiedReceiverIds, restrictedReceiverIds);
        }

        if (boardOwner != null
                && !support.isSameAccount(boardOwner, actor)
                && !support.isSameAccount(boardOwner, parentCommentOwner)
                && support.canReceiveNotification(boardOwner, restrictedReceiverIds)) {
            Notification saved = support.saveAndPublish(
                    Notification.boardComment(boardOwner, actor, board, comment, preview)
            );
            if (saved.getReceiver() != null && saved.getReceiver().getId() != null) {
                notifiedReceiverIds.add(saved.getReceiver().getId());
            }
        }

        for (Account mentionedUser : mentionedUsers) {
            if (mentionedUser.getId() == null || notifiedReceiverIds.contains(mentionedUser.getId())) {
                continue;
            }
            if (!support.canReceiveNotification(mentionedUser, restrictedReceiverIds)) {
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

    private void createReplyNotificationIfNeeded(Account receiver,
                                                 Board board,
                                                 Comments comment,
                                                 String preview,
                                                 Set<Long> notifiedReceiverIds,
                                                 Set<Long> restrictedReceiverIds) {
        if (receiver == null || !support.canReceiveNotification(receiver, restrictedReceiverIds)) {
            return;
        }
        Notification saved = support.saveAndPublish(
                Notification.commentReply(receiver, comment.getAccount(), board, comment, preview)
        );
        if (saved.getReceiver() != null && saved.getReceiver().getId() != null) {
            notifiedReceiverIds.add(saved.getReceiver().getId());
        }
    }
}
