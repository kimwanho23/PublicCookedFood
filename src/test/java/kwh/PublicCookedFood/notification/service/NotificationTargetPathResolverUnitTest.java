package kwh.PublicCookedFood.notification.service;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.service.comment.CommentTargetPath;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.notification.domain.Notification;
import kwh.PublicCookedFood.notification.domain.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationTargetPathResolverUnitTest {

    @Mock
    private NotificationCommentTargetPathService notificationCommentTargetPathService;

    private NotificationTargetPathResolver notificationTargetPathResolver;

    @BeforeEach
    void setUp() {
        notificationTargetPathResolver = new NotificationTargetPathResolver(notificationCommentTargetPathService);
    }

    @Test
    void resolveTargetPath_usesPrecomputedCommentPath() {
        Account receiver = account(1L, "receiver");
        Account actor = account(2L, "actor");
        Board board = board(10L, actor, false);
        Comments comment = comment(30L, actor, board);
        Notification notification = Notification.commentReply(receiver, actor, board, comment, "preview");
        CommentTargetPathIndex precomputed = CommentTargetPathIndex.of(
                Map.of(CommentTargetKey.of(10L, 30L), CommentTargetPath.of("/boards/10#comment-30"))
        );

        CommentTargetPath targetPath = notificationTargetPathResolver.resolveTargetPath(notification, 1L, precomputed);

        assertThat(targetPath).isEqualTo(CommentTargetPath.of("/boards/10#comment-30"));
    }

    @Test
    void precomputePaths_groupsRequestedCommentIdsByBoard() {
        Account receiver = account(1L, "receiver");
        Account actor = account(2L, "actor");
        Board board = board(10L, actor, false);
        Notification first = Notification.commentReply(receiver, actor, board, comment(30L, actor, board), "preview");
        Notification second = Notification.commentMention(receiver, actor, board, comment(31L, actor, board), "preview");
        when(notificationCommentTargetPathService.buildCommentTargetPaths(10L, List.of(30L, 31L), 1L))
                .thenReturn(Map.of(30L, CommentTargetPath.of("/boards/10#comment-30")));

        CommentTargetPathIndex targetPaths =
                notificationTargetPathResolver.precomputePaths(List.of(first, second), 1L);

        assertThat(targetPaths.find(10L, 30L)).contains(CommentTargetPath.of("/boards/10#comment-30"));
        assertThat(targetPaths.find(10L, 31L)).isEmpty();
    }

    @Test
    void resolveTargetPath_returnsBoardsPathForHiddenReportResult() {
        Account receiver = account(1L, "receiver");
        Account actor = account(2L, "actor");
        Board board = board(10L, actor, true);
        Notification notification = Notification.reportResult(
                receiver,
                actor,
                board,
                NotificationType.REPORT_RESOLVED,
                "preview"
        );

        CommentTargetPath targetPath = notificationTargetPathResolver.resolveTargetPath(
                notification,
                1L,
                CommentTargetPathIndex.empty()
        );

        assertThat(targetPath).isEqualTo(CommentTargetPath.of("/boards"));
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

    private Board board(Long id, Account owner, boolean hiddenByReport) {
        return Board.builder()
                .id(id)
                .account(owner)
                .state(SoftDeleteState.ACTIVE)
                .hiddenByReport(hiddenByReport)
                .build();
    }

    private Comments comment(Long id, Account actor, Board board) {
        return Comments.builder()
                .id(id)
                .account(actor)
                .board(board)
                .contents("comment")
                .build();
    }
}
