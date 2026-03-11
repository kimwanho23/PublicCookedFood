package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardReport;
import kwh.PublicCookedFood.board.domain.BoardReportReason;
import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.notification.domain.Notification;
import kwh.PublicCookedFood.notification.domain.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportProcessedNotificationDispatchStrategyUnitTest {

    @Mock
    private NotificationPublisher notificationPublisher;

    @Mock
    private NotificationPreviewFactory previewFactory;

    @Mock
    private NotificationReceiverPolicy receiverPolicy;

    @InjectMocks
    private ReportProcessedNotificationDispatchStrategy strategy;

    @Test
    void dispatch_buildsReportResultNotificationFromFallbackPreview() {
        Account reporter = account(1L, "reporter");
        Account processor = account(2L, "processor");
        Board board = Board.builder()
                .id(10L)
                .account(processor)
                .build();
        BoardReport report = BoardReport.builder()
                .id(20L)
                .board(board)
                .reporter(reporter)
                .processor(processor)
                .reason(BoardReportReason.SPAM)
                .status(BoardReportStatus.REJECTED)
                .processedNote(" ")
                .build();
        when(receiverPolicy.canReceiveFromActor(reporter, processor)).thenReturn(true);
        when(previewFactory.buildPreview(BoardReportReason.SPAM.getLabel())).thenReturn("preview");

        strategy.dispatch(ReportProcessedDispatchCommand.from(report));

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationPublisher).saveAndPublish(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getType()).isEqualTo(NotificationType.REPORT_REJECTED);
        assertThat(notification.getReceiver()).isSameAs(reporter);
        assertThat(notification.getActor()).isSameAs(processor);
        assertThat(notification.getBoard()).isSameAs(board);
        assertThat(notification.getContentPreview()).isEqualTo("preview");
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
