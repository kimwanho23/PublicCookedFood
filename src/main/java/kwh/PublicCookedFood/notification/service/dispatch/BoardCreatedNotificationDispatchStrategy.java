package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.notification.domain.Notification;
import kwh.PublicCookedFood.user.domain.Users;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class BoardCreatedNotificationDispatchStrategy implements NotificationDispatchStrategy {

    private final NotificationDispatchSupport support;

    @Override
    public NotificationDispatchType type() {
        return NotificationDispatchType.BOARD_CREATED;
    }

    @Override
    public void dispatch(NotificationDispatchContext context) {
        Board board = context.board();
        if (board == null || board.getUser() == null || board.getUser().getId() == null) {
            return;
        }

        Users actor = board.getUser();
        String plainText = Jsoup.parse(board.getContents() == null ? "" : board.getContents()).text();
        String preview = support.buildPreview(board.getTitle() == null ? plainText : board.getTitle() + " " + plainText);
        Set<Long> notifiedReceiverIds = new LinkedHashSet<>();

        for (Users mentionedUser : support.resolveMentionedUsers(plainText, actor.getId())) {
            if (mentionedUser.getId() == null || notifiedReceiverIds.contains(mentionedUser.getId())) {
                continue;
            }
            if (!support.canReceiveNotification(mentionedUser, actor)) {
                continue;
            }
            Notification saved = support.saveAndPublish(
                    Notification.boardMention(mentionedUser, actor, board, preview)
            );
            if (saved.getReceiver() != null && saved.getReceiver().getId() != null) {
                notifiedReceiverIds.add(saved.getReceiver().getId());
            }
        }
    }
}

