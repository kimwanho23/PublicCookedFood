package kwh.PublicCookedFood.notification.service;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.service.CommentNavigationService;
import kwh.PublicCookedFood.notification.service.NotificationTargetPathResolver;
import kwh.PublicCookedFood.notification.domain.Notification;
import kwh.PublicCookedFood.notification.dto.response.NotificationResponse;
import kwh.PublicCookedFood.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationViewSupportUnitTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AccountBlockService accountBlockService;

    @Mock
    private CommentNavigationService commentNavigationService;

    private NotificationTargetPathResolver notificationTargetPathResolver;
    private NotificationViewSupport notificationViewSupport;

    @BeforeEach
    void setUp() {
        notificationTargetPathResolver = new NotificationTargetPathResolver(commentNavigationService);
        notificationViewSupport = new NotificationViewSupport(
                notificationRepository,
                accountBlockService,
                notificationTargetPathResolver
        );
    }

    @Test
    void loadVisibleNotificationPage_returnsEmptyPageWhenRepositoryReturnsNoNotifications() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(accountBlockService.getViewRestrictedAccountIds(1L)).thenReturn(Set.of());
        when(notificationRepository.findVisibleNotificationsByCriteria(
                eq(1L),
                eq(false),
                eq(SoftDeleteState.ACTIVE),
                anyCollection(),
                eq(false),
                anyList(),
                eq(pageable)
        )).thenReturn(Page.empty(pageable));

        Page<NotificationResponse> result = notificationViewSupport.loadVisibleNotificationPage(1L, pageable, false);

        assertThat(result).isEmpty();
        assertThat(result.getPageable()).isEqualTo(pageable);
    }

    @Test
    void loadVisibleNotificationPage_usesPrecomputedCommentTargetPaths() {
        Account receiver = account(1L, "receiver");
        Account actor = account(2L, "actor");
        Account owner = account(3L, "owner");
        Board board = board(10L, owner, false);
        Comments comment = comment(30L, actor, board);
        Notification notification = Notification.commentReply(receiver, actor, board, comment, "preview");
        PageRequest pageable = PageRequest.of(0, 20);
        String targetPath = "/boards/10?commentPage=0&commentSize=50#comment-30";

        when(accountBlockService.getViewRestrictedAccountIds(1L)).thenReturn(Set.of());
        when(notificationRepository.findVisibleNotificationsByCriteria(
                eq(1L),
                eq(false),
                eq(SoftDeleteState.ACTIVE),
                anyCollection(),
                eq(false),
                anyList(),
                eq(pageable)
        )).thenReturn(new PageImpl<>(List.of(notification), pageable, 1));
        when(commentNavigationService.buildCommentTargetPaths(10L, List.of(30L), 1L))
                .thenReturn(Map.of(30L, targetPath));

        Page<NotificationResponse> result = notificationViewSupport.loadVisibleNotificationPage(1L, pageable, false);

        assertThat(result.getContent()).singleElement().satisfies(response -> {
            assertThat(response.actorId()).isEqualTo(2L);
            assertThat(response.boardId()).isEqualTo(10L);
            assertThat(response.commentId()).isEqualTo(30L);
            assertThat(response.targetPath()).isEqualTo(targetPath);
        });
    }

    @Test
    void countVisibleUnreadNotifications_usesVisibilityCriteriaForRestrictedViewer() {
        when(accountBlockService.getViewRestrictedAccountIds(1L)).thenReturn(Set.of(2L, 3L));
        when(notificationRepository.countVisibleNotificationsByCriteria(
                eq(1L),
                eq(true),
                eq(SoftDeleteState.ACTIVE),
                anyCollection(),
                eq(true),
                anyList()
        )).thenReturn(4L);

        long unreadCount = notificationViewSupport.countVisibleUnreadNotifications(1L);

        assertThat(unreadCount).isEqualTo(4L);
    }

    private Account account(Long accountId, String name) {
        return Account.builder()
                .id(accountId)
                .email(name + "@test.com")
                .name(name)
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }

    private Board board(Long boardId, Account owner, boolean hiddenByReport) {
        return Board.builder()
                .id(boardId)
                .title("board-" + boardId)
                .account(owner)
                .state(SoftDeleteState.ACTIVE)
                .hiddenByReport(hiddenByReport)
                .build();
    }

    private Comments comment(Long commentId, Account actor, Board board) {
        return Comments.builder()
                .id(commentId)
                .account(actor)
                .board(board)
                .contents("comment")
                .build();
    }
}
