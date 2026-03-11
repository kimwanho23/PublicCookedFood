package kwh.PublicCookedFood.notification.service;

import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.account.error.AccountErrorCode;
import kwh.PublicCookedFood.notification.repository.NotificationRepository;
import kwh.PublicCookedFood.notification.error.NotificationErrorCode;
import kwh.PublicCookedFood.notification.service.dispatch.BoardCreatedNotificationDispatchStrategy;
import kwh.PublicCookedFood.notification.service.dispatch.CommentReplyTarget;
import kwh.PublicCookedFood.notification.service.dispatch.NewCommentNotificationDispatchStrategy;
import kwh.PublicCookedFood.notification.service.dispatch.ReportProcessedNotificationDispatchStrategy;
import kwh.PublicCookedFood.account.audit.NotificationAuditPublisher;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardReport;
import kwh.PublicCookedFood.board.domain.BoardReportReason;
import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceUnitTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private NewCommentNotificationDispatchStrategy newCommentStrategy;

    @Mock
    private BoardCreatedNotificationDispatchStrategy boardCreatedStrategy;

    @Mock
    private ReportProcessedNotificationDispatchStrategy reportProcessedStrategy;

    @Mock
    private NotificationAuditPublisher notificationAuditPublisher;

    @Mock
    private NotificationSseService notificationSseService;

    @Mock
    private NotificationViewSupport notificationViewSupport;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                accountRepository,
                notificationAuditPublisher,
                newCommentStrategy,
                boardCreatedStrategy,
                reportProcessedStrategy,
                notificationSseService,
                notificationViewSupport
        );
    }

    @Test
    void updateNotificationEnabled_disableClearsActiveEmitters() {
        Account account = createAccount();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        boolean enabled = notificationService.updateNotificationEnabled(1L, false);

        assertThat(enabled).isFalse();
        assertThat(account.isNotificationEnabled()).isFalse();
        verify(notificationAuditPublisher).notificationSettingUpdate(1L, false);
        verify(notificationSseService).clearEmittersAfterCommit(1L);
    }

    @Test
    void subscribe_throwsWhenNotificationSettingDisabled() {
        when(notificationSseService.isSseEnabled()).thenReturn(false);

        assertThatThrownBy(() -> notificationService.subscribe(2L))
                .isInstanceOfSatisfying(AppException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(NotificationErrorCode.NOTIFICATION_SSE_DISABLED);
                    assertThat(e.getMessage()).contains("비활성화");
                });
    }

    @Test
    void subscribe_registersEmitterWhenNotificationEnabled() {
        when(notificationSseService.isSseEnabled()).thenReturn(true);
        SseEmitter expected = new SseEmitter();
        when(notificationSseService.subscribe(3L)).thenReturn(expected);

        SseEmitter emitter = notificationService.subscribe(3L);

        assertThat(emitter).isSameAs(expected);
    }

    @Test
    void markAsRead_throwsWhenNotificationIsMissingOrNotOwned() {
        when(notificationRepository.markAsRead(eq(99L), eq(1L), any()))
                .thenReturn(0);
        when(notificationRepository.existsByIdAndReceiverId(99L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 99L))
                .isInstanceOfSatisfying(AppException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
    }

    @Test
    void markAsRead_isIdempotentWhenNotificationAlreadyRead() {
        when(notificationRepository.markAsRead(eq(99L), eq(1L), any()))
                .thenReturn(0);
        when(notificationRepository.existsByIdAndReceiverId(99L, 1L)).thenReturn(true);

        notificationService.markAsRead(1L, 99L);

        verify(notificationRepository).existsByIdAndReceiverId(99L, 1L);
    }

    @Test
    void deleteNotification_throwsWhenNotificationIsMissingOrNotOwned() {
        when(notificationRepository.deleteByIdAndReceiverId(99L, 1L)).thenReturn(0L);

        assertThatThrownBy(() -> notificationService.deleteNotification(1L, 99L))
                .isInstanceOfSatisfying(AppException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
    }

    @Test
    void updateNotificationEnabled_throwsAccountErrorWhenAccountMissing() {
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.updateNotificationEnabled(1L, false))
                .isInstanceOfSatisfying(AppException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AccountErrorCode.ACCOUNT_NOT_FOUND));
    }

    @Test
    void isNotificationEnabled_throwsAccountErrorWhenAccountMissing() {
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.isNotificationEnabled(1L))
                .isInstanceOfSatisfying(AppException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AccountErrorCode.ACCOUNT_NOT_FOUND));
    }

    @Test
    void notifyOnNewComment_buildsValidatedCommandAndDelegates() {
        Account actor = account(1L, "actor");
        Board board = Board.builder()
                .id(10L)
                .account(account(2L, "board-owner"))
                .build();
        Comments comment = Comments.builder()
                .id(20L)
                .account(actor)
                .board(board)
                .contents("hello")
                .build();

        notificationService.notifyOnNewComment(comment);

        verify(newCommentStrategy).dispatch(argThat(command ->
                command.comment() == comment
                        && command.replyTarget() instanceof CommentReplyTarget.Root
        ));
    }

    @Test
    void notifyOnBoardCreated_delegatesBoard() {
        Account actor = account(1L, "actor");
        Board board = Board.builder()
                .id(10L)
                .account(actor)
                .build();

        notificationService.notifyOnBoardCreated(board);

        verify(boardCreatedStrategy).dispatch(board);
    }

    @Test
    void notifyOnReportProcessed_buildsValidatedCommandAndDelegates() {
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
                .status(BoardReportStatus.RESOLVED)
                .build();

        notificationService.notifyOnReportProcessed(report);

        verify(reportProcessedStrategy).dispatch(argThat(command ->
                command.report() == report
                        && command.reporter() == reporter
                        && command.processor() == processor
                        && command.notificationType() == kwh.PublicCookedFood.notification.domain.NotificationType.REPORT_RESOLVED
        ));
    }

    private Account createAccount() {
        return account(1L, "알림테스터");
    }

    private Account account(Long id, String name) {
        return Account.builder()
                .id(id)
                .email("notification-" + id + "@test.com")
                .name(name)
                .notificationEnabled(true)
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }

}
