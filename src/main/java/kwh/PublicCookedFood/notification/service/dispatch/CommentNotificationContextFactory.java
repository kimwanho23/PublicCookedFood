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
        List<Account> mentionedUsers = mentionResolver.resolveMentionedUsers(
                comment.getContents(),
                command.actor().getId()
        );
        RestrictedReceivers restrictedReceivers = receiverPolicy.resolveRestrictedReceivers(
                command.actor(),
                collectCandidateReceivers(command, mentionedUsers)
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
                                                   List<Account> mentionedUsers) {
        Set<Account> candidateReceivers = new LinkedHashSet<>(mentionedUsers);
        if (command.replyTarget() instanceof CommentReplyTarget.Reply reply
                && !receiverPolicy.isSameAccount(reply.owner(), command.actor())) {
            candidateReceivers.add(reply.owner());
        }

        boolean boardOwnerIsReplyOwner = command.replyTarget() instanceof CommentReplyTarget.Reply reply
                && receiverPolicy.isSameAccount(command.boardOwner(), reply.owner());
        if (!receiverPolicy.isSameAccount(command.boardOwner(), command.actor()) && !boardOwnerIsReplyOwner) {
            candidateReceivers.add(command.boardOwner());
        }
        return candidateReceivers;
    }
}
