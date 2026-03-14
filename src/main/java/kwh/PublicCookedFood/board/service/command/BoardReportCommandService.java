package kwh.PublicCookedFood.board.service.command;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardReport;
import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.board.error.BoardErrorCode;
import kwh.PublicCookedFood.board.repository.BoardReportRepository;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.service.BoardReportPolicy;
import kwh.PublicCookedFood.board.service.BoardReportTextSanitizer;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.CommonErrorCode;
import kwh.PublicCookedFood.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BoardReportCommandService {

    private final BoardReportRepository boardReportRepository;
    private final BoardRepository boardRepository;
    private final AccountRepository accountRepository;
    private final NotificationService notificationService;
    private final BoardReportPolicy boardReportPolicy;
    private final BoardReportTextSanitizer boardReportTextSanitizer;

    @Transactional
    public void createReport(BoardReportCreateCommand command) {
        boardReportPolicy.validateCreateRequest(command);
        boardReportPolicy.ensureNotReported(command);

        LocalDateTime now = LocalDateTime.now();
        BoardReportPolicy.ReporterReportVolume reporterVolume =
                boardReportPolicy.loadReporterVolume(command.reporterId(), now);
        boardReportPolicy.enforceRateLimit(reporterVolume);

        Account reporter = accountRepository.findById(command.reporterId())
                .orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND, "유효하지 않은 사용자입니다."));
        Board board = boardRepository.findById(command.boardId())
                .orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND, "유효하지 않은 게시글입니다."));
        boardReportPolicy.ensureReportable(board, command.reporterId());
        BoardReportPolicy.ReportPriority reportPriority = boardReportPolicy.assessPriority(command, reporterVolume);

        try {
            String sanitizedDetails = boardReportTextSanitizer.sanitizeDetails(command.details()).orElse(null);
            BoardReport savedReport = boardReportRepository.saveAndFlush(BoardReport.builder()
                    .board(board)
                    .reporter(reporter)
                    .reason(command.reason())
                    .details(sanitizedDetails)
                    .status(BoardReportStatus.OPEN)
                    .priorityScore(reportPriority.score())
                    .suspicious(reportPriority.suspicious())
                    .build());
            boardReportPolicy.syncBoardVisibility(savedReport.getBoard());
        } catch (DataIntegrityViolationException e) {
            throw new AppException(BoardErrorCode.BOARD_REPORT_DUPLICATED);
        }
    }

    @Transactional
    public void updateReportStatus(BoardReportStatusUpdateCommand command) {
        BoardReport report = boardReportRepository.findById(command.reportId())
                .orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND, "신고 내역을 찾을 수 없습니다."));
        BoardReportStatus previousStatus = report.getStatus();

        Account processor = null;
        if (command.status() != BoardReportStatus.OPEN) {
            processor = accountRepository.findById(command.processorId())
                    .orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND, "처리자 정보를 찾을 수 없습니다."));
        }

        report.updateStatus(
                command.status(),
                processor,
                boardReportTextSanitizer.sanitizeProcessNote(command.processNote()).orElse(null),
                LocalDateTime.now());
        boardReportPolicy.syncBoardVisibility(report.getBoard());

        if (previousStatus != command.status() && command.status() != BoardReportStatus.OPEN) {
            notificationService.notifyOnReportProcessed(report);
        }
    }
}
