package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.notification.domain.Notification;
import kwh.PublicCookedFood.notification.repository.NotificationRepository;
import kwh.PublicCookedFood.notification.service.NotificationSseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPublisherUnitTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationSseService notificationSseService;

    private NotificationPublisher notificationPublisher;

    @BeforeEach
    void setUp() {
        notificationPublisher = new NotificationPublisher(notificationRepository, notificationSseService);
    }

    @Test
    void publishAll_publishesEveryNotificationEvenForSameReceiver() {
        Account receiver = account();
        Account actor = actor();
        Board board = board(actor);
        Comments comment = comment(actor, board);
        Notification first = Notification.commentReply(receiver, actor, board, comment, "reply");
        Notification duplicate = Notification.commentMention(receiver, actor, board, comment, "mention");
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationPublisher.publishAll(List.of(first, duplicate));

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void saveAndPublish_publishesAfterCommitWhenSavedNotificationHasIds() {
        Account receiver = account();
        Account actor = actor();
        Board board = board(actor);
        Notification saved = Notification.boardMention(receiver, actor, board, "preview");
        ReflectionTestUtils.setField(saved, "id", 99L);
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        notificationPublisher.saveAndPublish(saved);

        verify(notificationSseService).publishNotificationAfterCommit(eq(10L), eq(99L));
    }

    private Account account() {
        return Account.builder()
                .id(10L)
                .email("tester10@test.com")
                .name("tester")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }

    private Account actor() {
        return Account.builder()
                .id(20L)
                .email("actor@test.com")
                .name("actor")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }

    private Board board(Account actor) {
        return Board.builder()
                .id(30L)
                .account(actor)
                .build();
    }

    private Comments comment(Account actor, Board board) {
        return Comments.builder()
                .id(40L)
                .account(actor)
                .board(board)
                .contents("comment")
                .build();
    }
}
