package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.board.domain.Comments;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
class CommentNotificationContextFactory {

    private final NotificationMentionResolver mentionResolver;
    private final NotificationReceiverPolicy receiverPolicy;
    private final NotificationPreviewFactory previewFactory;

    CommentNotificationContext create(NewCommentDispatchCommand command) {
        Comments comment = command.comment();
        Account actor = comment.getAccount();
        List<Account> mentionedUsers = mentionResolver.resolveMentionedUsers(
                comment.getContents(),
                actor.getId()
        );
        NotificationReceiverPolicy.RestrictedReceivers restrictedReceivers = receiverPolicy.resolveRestrictedReceivers(
                actor,
                collectCandidateReceivers(command, actor, mentionedUsers)
        );
        return new CommentNotificationContext(
                comment,
                command.replyTarget(),
                mentionedUsers,
                restrictedReceivers,
                previewFactory.buildPreview(comment.getContents())
        );
    }

    private Set<Account> collectCandidateReceivers(NewCommentDispatchCommand command,
                                                   Account actor,
                                                   List<Account> mentionedUsers) {
        Account boardOwner = command.comment().getBoard().getAccount();
        Set<Account> candidateReceivers = new LinkedHashSet<>(mentionedUsers);
        if (command.replyTarget() instanceof CommentReplyTarget.Reply reply
                && !NotificationReceiverPolicy.isSameAccount(reply.owner(), actor)) {
            candidateReceivers.add(reply.owner());
        }

        boolean boardOwnerIsReplyOwner = command.replyTarget() instanceof CommentReplyTarget.Reply reply
                && NotificationReceiverPolicy.isSameAccount(boardOwner, reply.owner());
        if (!NotificationReceiverPolicy.isSameAccount(boardOwner, actor) && !boardOwnerIsReplyOwner) {
            candidateReceivers.add(boardOwner);
        }
        return candidateReceivers;
    }
}
