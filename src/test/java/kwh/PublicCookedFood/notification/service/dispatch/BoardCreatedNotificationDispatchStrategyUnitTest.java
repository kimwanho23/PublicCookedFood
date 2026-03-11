package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.board.domain.Board;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardCreatedNotificationDispatchStrategyUnitTest {

    @Mock
    private NotificationMentionResolver mentionResolver;

    @Mock
    private NotificationPreviewFactory previewFactory;

    @Mock
    private NotificationReceiverPolicy receiverPolicy;

    @Mock
    private NotificationPublisher notificationPublisher;

    @Test
    void dispatch_buildsPreviewFromTitleAndPlainText() {
        BoardCreatedNotificationDispatchStrategy strategy = new BoardCreatedNotificationDispatchStrategy(
                mentionResolver,
                previewFactory,
                receiverPolicy,
                notificationPublisher
        );
        Account actor = Account.builder()
                .id(1L)
                .email("actor@test.com")
                .name("actor")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
        Board board = Board.builder()
                .id(10L)
                .title("title")
                .contents("<p>body</p>")
                .account(actor)
                .build();
        when(previewFactory.extractPlainText("<p>body</p>")).thenReturn("body");
        when(previewFactory.buildBoardPreview("title", "<p>body</p>")).thenReturn("title body");
        when(mentionResolver.resolveMentionedUsers("body", 1L)).thenReturn(List.of());

        strategy.dispatch(board);

        verify(previewFactory).extractPlainText("<p>body</p>");
        verify(previewFactory).buildBoardPreview("title", "<p>body</p>");
        verify(mentionResolver).resolveMentionedUsers("body", 1L);
        verify(notificationPublisher).publishAll(java.util.List.of());
    }
}
