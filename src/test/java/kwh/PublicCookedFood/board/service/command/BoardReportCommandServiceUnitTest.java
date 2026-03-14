package kwh.PublicCookedFood.board.service.command;

import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.board.domain.BoardReportReason;
import kwh.PublicCookedFood.board.error.BoardErrorCode;
import kwh.PublicCookedFood.board.repository.BoardReportRepository;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.service.BoardReportPolicy;
import kwh.PublicCookedFood.board.service.BoardReportTextSanitizer;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.CommonErrorCode;
import kwh.PublicCookedFood.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardReportCommandServiceUnitTest {

    @Mock
    private BoardReportRepository boardReportRepository;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private BoardReportPolicy boardReportPolicy;

    @Mock
    private BoardReportTextSanitizer boardReportTextSanitizer;

    private BoardReportCommandService boardReportCommandService;

    @BeforeEach
    void setUp() {
        boardReportCommandService = new BoardReportCommandService(
                boardReportRepository,
                boardRepository,
                accountRepository,
                notificationService,
                boardReportPolicy,
                boardReportTextSanitizer
        );
    }

    @Test
    void createReport_throwsAppExceptionWhenReportAlreadyExists() {
        BoardReportCreateCommand command = new BoardReportCreateCommand(10L, 1L, BoardReportReason.SPAM, null);
        org.mockito.Mockito.doThrow(new AppException(BoardErrorCode.BOARD_REPORT_DUPLICATED))
                .when(boardReportPolicy).ensureNotReported(command);

        assertThatThrownBy(() -> boardReportCommandService.createReport(command))
                .isInstanceOfSatisfying(AppException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(BoardErrorCode.BOARD_REPORT_DUPLICATED));

        verifyNoInteractions(accountRepository, boardRepository, notificationService);
    }

    @Test
    void createReport_throwsAppExceptionWhenReporterExceededBurstLimit() {
        BoardReportCreateCommand command = new BoardReportCreateCommand(10L, 1L, BoardReportReason.SPAM, null);
        when(boardReportPolicy.loadReporterVolume(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new BoardReportPolicy.ReporterReportVolume(5L, 0L));
        org.mockito.Mockito.doThrow(new AppException(BoardErrorCode.BOARD_REPORT_RATE_LIMITED))
                .when(boardReportPolicy).enforceRateLimit(new BoardReportPolicy.ReporterReportVolume(5L, 0L));

        assertThatThrownBy(() -> boardReportCommandService.createReport(command))
                .isInstanceOfSatisfying(AppException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(BoardErrorCode.BOARD_REPORT_RATE_LIMITED));

        verifyNoInteractions(accountRepository, boardRepository, notificationService);
    }

    @Test
    void createReport_throwsAppExceptionWhenReporterDoesNotExist() {
        BoardReportCreateCommand command = new BoardReportCreateCommand(10L, 1L, BoardReportReason.SPAM, null);
        when(boardReportPolicy.loadReporterVolume(org.mockito.ArgumentMatchers.eq(1L), any()))
                .thenReturn(new BoardReportPolicy.ReporterReportVolume(0L, 0L));
        when(accountRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> boardReportCommandService.createReport(command))
                .isInstanceOfSatisfying(AppException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
                    assertThat(e).hasMessageContaining("유효하지 않은 사용자");
                });
    }
}
