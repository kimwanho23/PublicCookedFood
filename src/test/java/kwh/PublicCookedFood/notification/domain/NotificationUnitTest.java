package kwh.PublicCookedFood.notification.domain;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationUnitTest {

    @Test
    void boardMention_exposesBoardTarget() {
        Account receiver = account(1L, "receiver");
        Account actor = account(2L, "actor");
        Board board = board(actor);

        Notification notification = Notification.boardMention(receiver, actor, board, "preview");

        assertThat(notification.target()).isInstanceOf(BoardNotificationTarget.class);
        assertThat(((BoardNotificationTarget) notification.target()).boardId()).isEqualTo(10L);
    }

    @Test
    void commentReply_exposesCommentTarget() {
        Account receiver = account(1L, "receiver");
        Account actor = account(2L, "actor");
        Board board = board(actor);
        Comments comment = comment(actor, board);

        Notification notification = Notification.commentReply(receiver, actor, board, comment, "preview");

        assertThat(notification.target()).isInstanceOf(CommentNotificationTarget.class);
        assertThat(((CommentNotificationTarget) notification.target()).commentId()).isEqualTo(20L);
    }

    @Test
    void builder_rejectsMissingCommentForCommentNotification() {
        Account receiver = account(1L, "receiver");
        Account actor = account(2L, "actor");
        Board board = board(actor);

        assertThatThrownBy(() -> Notification.builder()
                .receiver(receiver)
                .actor(actor)
                .board(board)
                .type(NotificationType.COMMENT_REPLY)
                .contentPreview("preview")
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("comment");
    }

    private Account account(Long id, String name) {
        return Account.builder()
                .id(id)
                .email(name + "@test.com")
                .name(name)
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }

    private Board board(Account owner) {
        return Board.builder()
                .id(10L)
                .account(owner)
                .build();
    }

    private Comments comment(Account actor, Board board) {
        return Comments.builder()
                .id(20L)
                .account(actor)
                .board(board)
                .contents("comment")
                .build();
    }
}
