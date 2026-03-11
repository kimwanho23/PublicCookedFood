package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.common.util.ImmutableCollections;

import java.util.List;

record CommentNotificationContext(Comments comment,
                                  CommentReplyTarget replyTarget,
                                  List<Account> mentionedUsers,
                                  NotificationReceiverPolicy.RestrictedReceivers restrictedReceivers,
                                  String preview) {

    CommentNotificationContext {
        mentionedUsers = ImmutableCollections.immutableList(mentionedUsers);
    }

    Account actor() {
        return comment.getAccount();
    }

    Board board() {
        return comment.getBoard();
    }

    Account boardOwner() {
        return board().getAccount();
    }

    boolean hasReplyOwner() {
        return replyTarget instanceof CommentReplyTarget.Reply;
    }

    Account replyOwner() {
        return ((CommentReplyTarget.Reply) replyTarget).owner();
    }

    boolean isActor(Account candidate) {
        return NotificationReceiverPolicy.isSameAccount(candidate, actor());
    }

    boolean isBoardOwnerAlsoReplyOwner() {
        return hasReplyOwner() && NotificationReceiverPolicy.isSameAccount(boardOwner(), replyOwner());
    }

    boolean canNotify(Account receiver) {
        return receiver.isNotificationEnabled() && restrictedReceivers.allows(receiver);
    }
}
