package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.board.domain.BoardReportReason;
import kwh.PublicCookedFood.board.error.BoardErrorCode;
import kwh.PublicCookedFood.board.repository.BoardReportRepository;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.notification.service.NotificationService;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardReportServiceUnitTest {

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

    @InjectMocks
    private BoardReportService boardReportService;

    @Test
    void createReport_throwsAppExceptionWhenReportAlreadyExists() {
        org.mockito.Mockito.doThrow(new AppException(BoardErrorCode.BOARD_REPORT_DUPLICATED))
                .when(boardReportPolicy).ensureNotReported(10L, 1L);

        assertThatThrownBy(() -> boardReportService.createReport(10L, 1L, BoardReportReason.SPAM, null))
                .isInstanceOfSatisfying(AppException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(BoardErrorCode.BOARD_REPORT_DUPLICATED));

        verifyNoInteractions(accountRepository, boardRepository, notificationService);
    }

    @Test
    void createReport_throwsAppExceptionWhenReporterExceededBurstLimit() {
        when(boardReportPolicy.loadReporterVolume(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new BoardReportPolicy.ReporterReportVolume(5L, 0L));
        org.mockito.Mockito.doThrow(new AppException(BoardErrorCode.BOARD_REPORT_RATE_LIMITED))
                .when(boardReportPolicy).enforceRateLimit(new BoardReportPolicy.ReporterReportVolume(5L, 0L));

        assertThatThrownBy(() -> boardReportService.createReport(10L, 1L, BoardReportReason.SPAM, null))
                .isInstanceOfSatisfying(AppException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(BoardErrorCode.BOARD_REPORT_RATE_LIMITED));

        verifyNoInteractions(accountRepository, boardRepository, notificationService);
    }
}
