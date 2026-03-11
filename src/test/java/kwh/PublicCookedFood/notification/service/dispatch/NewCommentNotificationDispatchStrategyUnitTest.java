package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.notification.domain.Notification;
import kwh.PublicCookedFood.notification.domain.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewCommentNotificationDispatchStrategyUnitTest {

    @Mock
    private NotificationMentionResolver mentionResolver;

    @Mock
    private NotificationReceiverPolicy receiverPolicy;

    @Mock
    private NotificationPreviewFactory previewFactory;

    @Mock
    private NotificationPublisher notificationPublisher;

    private NewCommentNotificationDispatchStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new NewCommentNotificationDispatchStrategy(
                new CommentNotificationContextFactory(mentionResolver, receiverPolicy, previewFactory),
                new CommentNotificationPlanner(),
                notificationPublisher
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void dispatch_plansSingleNotificationPerReceiverWithExplicitPriority() {
        Account actor = account(1L, "actor");
        Account parentOwner = account(2L, "parent");
        Account boardOwner = account(3L, "board");
        Account mentionedUser = account(4L, "mention");
        Board board = Board.builder()
                .id(10L)
                .account(boardOwner)
                .build();
        Comments parentComment = Comments.builder()
                .id(20L)
                .account(parentOwner)
                .board(board)
                .contents("parent")
                .build();
        Comments comment = Comments.builder()
                .id(21L)
                .account(actor)
                .board(board)
                .parent(parentComment)
                .contents("@parent @board @mention hello")
                .build();
        when(mentionResolver.resolveMentionedUsers(comment.getContents(), actor.getId()))
                .thenReturn(List.of(parentOwner, boardOwner, mentionedUser));
        when(receiverPolicy.resolveRestrictedReceivers(eq(actor), anySet())).thenReturn(RestrictedReceivers.empty());
        when(previewFactory.buildPreview(comment.getContents())).thenReturn("preview");
        when(receiverPolicy.isSameAccount(any(), any())).thenAnswer(invocation -> sameAccount(
                invocation.getArgument(0, Account.class),
                invocation.getArgument(1, Account.class)
        ));

        strategy.dispatch(new NewCommentDispatchCommand(
                comment,
                CommentReplyTarget.reply(parentOwner)
        ));

        ArgumentCaptor<Iterable<Notification>> notificationsCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(notificationPublisher).publishAll(notificationsCaptor.capture());
        List<Notification> notifications = StreamSupport.stream(notificationsCaptor.getValue().spliterator(), false)
                .toList();

        assertThat(notifications)
                .extracting(Notification::getType)
                .containsExactly(
                        NotificationType.COMMENT_REPLY,
                        NotificationType.BOARD_COMMENT,
                        NotificationType.COMMENT_MENTION
                );
        assertThat(notifications)
                .extracting(notification -> notification.getReceiver().getId())
                .containsExactly(2L, 3L, 4L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void dispatch_keepsHighestPriorityNotificationWhenReceiverOverlapsAcrossRules() {
        Account actor = account(1L, "actor");
        Account owner = account(2L, "owner");
        Board board = Board.builder()
                .id(10L)
                .account(owner)
                .build();
        Comments parentComment = Comments.builder()
                .id(20L)
                .account(owner)
                .board(board)
                .contents("parent")
                .build();
        Comments comment = Comments.builder()
                .id(21L)
                .account(actor)
                .board(board)
                .parent(parentComment)
                .contents("@owner hello")
                .build();
        when(mentionResolver.resolveMentionedUsers(comment.getContents(), actor.getId()))
                .thenReturn(List.of(owner));
        when(receiverPolicy.resolveRestrictedReceivers(eq(actor), anySet())).thenReturn(RestrictedReceivers.empty());
        when(previewFactory.buildPreview(comment.getContents())).thenReturn("preview");
        when(receiverPolicy.isSameAccount(any(), any())).thenAnswer(invocation -> sameAccount(
                invocation.getArgument(0, Account.class),
                invocation.getArgument(1, Account.class)
        ));

        strategy.dispatch(new NewCommentDispatchCommand(
                comment,
                CommentReplyTarget.reply(owner)
        ));

        ArgumentCaptor<Iterable<Notification>> notificationsCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(notificationPublisher).publishAll(notificationsCaptor.capture());
        List<Notification> notifications = StreamSupport.stream(notificationsCaptor.getValue().spliterator(), false)
                .toList();

        assertThat(notifications)
                .singleElement()
                .satisfies(notification -> {
                    assertThat(notification.getType()).isEqualTo(NotificationType.COMMENT_REPLY);
                    assertThat(notification.getReceiver().getId()).isEqualTo(2L);
                });
    }

    private boolean sameAccount(Account left, Account right) {
        return left != null
                && right != null
                && left.getId() != null
                && left.getId().equals(right.getId());
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
}
