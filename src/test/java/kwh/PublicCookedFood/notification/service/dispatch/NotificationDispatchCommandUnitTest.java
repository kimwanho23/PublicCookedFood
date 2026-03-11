package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardReport;
import kwh.PublicCookedFood.board.domain.BoardReportReason;
import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.notification.domain.NotificationType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationDispatchCommandUnitTest {

    @Test
    void newCommentDispatchCommand_fromDerivesReplyTargetFromCommentParent() {
        Account actor = account(1L, "actor");
        Account parentOwner = account(2L, "parent");
        Board board = board(actor);
        Comments parent = comment(20L, parentOwner, board, null);
        Comments reply = comment(21L, actor, board, parent);

        NewCommentDispatchCommand command = NewCommentDispatchCommand.from(reply);

        assertThat(command.comment()).isSameAs(reply);
        assertThat(command.replyTarget()).isInstanceOf(CommentReplyTarget.Reply.class);
        assertThat(((CommentReplyTarget.Reply) command.replyTarget()).owner()).isSameAs(parentOwner);
    }

    @Test
    void newCommentDispatchCommand_rejectsReplyTargetThatDoesNotMatchCommentParent() {
        Account actor = account(1L, "actor");
        Account parentOwner = account(2L, "parent");
        Board board = board(actor);
        Comments parent = comment(20L, parentOwner, board, null);
        Comments reply = comment(21L, actor, board, parent);

        assertThatThrownBy(() -> new NewCommentDispatchCommand(reply, CommentReplyTarget.root()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("답글");
    }

    @Test
    void newCommentDispatchCommand_acceptsReplyTargetWhenParentOwnerHasSameIdButDifferentInstance() {
        Account actor = account(1L, "actor");
        Account parentOwner = account(2L, "parent");
        Account reloadedParentOwner = account(2L, "parent-reloaded");
        Board board = board(actor);
        Comments parent = comment(20L, parentOwner, board, null);
        Comments reply = comment(21L, actor, board, parent);

        NewCommentDispatchCommand command = new NewCommentDispatchCommand(
                reply,
                CommentReplyTarget.reply(reloadedParentOwner)
        );

        assertThat(command.replyTarget()).isEqualTo(CommentReplyTarget.reply(reloadedParentOwner));
    }

    @Test
    void reportProcessedDispatchCommand_fromDerivesNotificationTypeAndPreviewSeed() {
        Account reporter = account(1L, "reporter");
        Account processor = account(2L, "processor");
        Board board = board(processor);
        BoardReport report = BoardReport.builder()
                .id(30L)
                .board(board)
                .reporter(reporter)
                .processor(processor)
                .reason(BoardReportReason.SPAM)
                .status(BoardReportStatus.RESOLVED)
                .processedNote("처리")
                .build();

        ReportProcessedDispatchCommand command = ReportProcessedDispatchCommand.from(report);

        assertThat(command.report()).isSameAs(report);
        assertThat(command.notificationType()).isEqualTo(NotificationType.REPORT_RESOLVED);
        assertThat(command.previewSeed()).isEqualTo("처리");
    }

    @Test
    void reportProcessedDispatchCommand_fromFallsBackToReasonLabelWhenProcessedNoteIsBlank() {
        Account reporter = account(1L, "reporter");
        Account processor = account(2L, "processor");
        Board board = board(processor);
        BoardReport report = BoardReport.builder()
                .id(30L)
                .board(board)
                .reporter(reporter)
                .processor(processor)
                .reason(BoardReportReason.SPAM)
                .status(BoardReportStatus.REJECTED)
                .processedNote(" ")
                .build();

        ReportProcessedDispatchCommand command = ReportProcessedDispatchCommand.from(report);

        assertThat(command.notificationType()).isEqualTo(NotificationType.REPORT_REJECTED);
        assertThat(command.previewSeed()).isEqualTo(BoardReportReason.SPAM.getLabel());
    }

    @Test
    void reportProcessedDispatchCommand_rejectsNotificationTypeThatDoesNotMatchReportStatus() {
        Account reporter = account(1L, "reporter");
        Account processor = account(2L, "processor");
        Board board = board(processor);
        BoardReport report = BoardReport.builder()
                .id(30L)
                .board(board)
                .reporter(reporter)
                .processor(processor)
                .reason(BoardReportReason.SPAM)
                .status(BoardReportStatus.RESOLVED)
                .processedNote("처리")
                .build();

        assertThatThrownBy(() -> new ReportProcessedDispatchCommand(
                report,
                NotificationType.REPORT_REJECTED,
                "preview"
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("신고 처리 결과");
    }

    private Account account(Long id, String name) {
        return Account.builder()
                .id(id)
                .email(name + "@test.com")
                .name(name)
                .authority(Role.USER)
                .notificationEnabled(true)
                .loginMethod("Current")
                .build();
    }

    private Board board(Account owner) {
        return Board.builder()
                .id(10L)
                .account(owner)
                .build();
    }

    private Comments comment(Long id, Account actor, Board board, Comments parent) {
        return Comments.builder()
                .id(id)
                .account(actor)
                .board(board)
                .parent(parent)
                .contents("comment")
                .build();
    }
}
