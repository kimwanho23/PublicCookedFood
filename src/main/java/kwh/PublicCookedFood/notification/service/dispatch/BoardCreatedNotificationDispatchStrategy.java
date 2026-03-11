package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.notification.domain.Notification;
import kwh.PublicCookedFood.account.domain.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BoardCreatedNotificationDispatchStrategy {

    private final NotificationMentionResolver mentionResolver;
    private final NotificationPreviewFactory previewFactory;
    private final NotificationReceiverPolicy receiverPolicy;
    private final NotificationPublisher notificationPublisher;

    public void dispatch(Board board) {
        Account actor = board.getAccount();
        String plainText = previewFactory.extractPlainText(board.getContents());
        String preview = previewFactory.buildBoardPreview(board.getTitle(), board.getContents());
        List<Notification> notifications = mentionResolver.resolveMentionedUsers(plainText, actor.getId()).stream()
                .filter(mentionedUser -> receiverPolicy.canReceiveFromActor(mentionedUser, actor))
                .map(mentionedUser -> Notification.boardMention(mentionedUser, actor, board, preview))
                .toList();

        notificationPublisher.publishAll(notifications);
    }
}
