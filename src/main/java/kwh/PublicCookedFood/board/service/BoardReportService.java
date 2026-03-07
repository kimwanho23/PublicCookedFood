package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardReport;
import kwh.PublicCookedFood.board.domain.BoardReportReason;
import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.board.error.BoardErrorCode;
import kwh.PublicCookedFood.board.repository.BoardReportRepository;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.notification.service.NotificationService;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BoardReportService {

    private final BoardReportRepository boardReportRepository;
    private final BoardRepository boardRepository;
    private final AccountRepository accountRepository;
    private final NotificationService notificationService;
    private final BoardReportPolicy boardReportPolicy;
    private final BoardReportTextSanitizer boardReportTextSanitizer;

    @Transactional
    public void createReport(Long boardId, Long reporterId, BoardReportReason reason, String details) {
        boardReportPolicy.validateCreateRequest(boardId, reporterId, reason, details);
        boardReportPolicy.ensureNotReported(boardId, reporterId);

        LocalDateTime now = LocalDateTime.now();
        BoardReportPolicy.ReporterReportVolume reporterVolume = boardReportPolicy.loadReporterVolume(reporterId, now);
        boardReportPolicy.enforceRateLimit(reporterVolume);

        Account reporter = accountRepository.findById(reporterId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자입니다."));
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 게시글입니다."));
        boardReportPolicy.ensureReportable(board, reporterId);
        BoardReportPolicy.ReportPriority reportPriority = boardReportPolicy.assessPriority(reporterId, reason, reporterVolume);

        try {
            BoardReport savedReport = boardReportRepository.saveAndFlush(BoardReport.builder()
                    .board(board)
                    .reporter(reporter)
                    .reason(reason)
                    .details(boardReportTextSanitizer.sanitizeDetails(details))
                    .status(BoardReportStatus.OPEN)
                    .priorityScore(reportPriority.score())
                    .suspicious(reportPriority.suspicious())
                    .build());
            boardReportPolicy.syncBoardVisibility(savedReport.getBoard());
        } catch (DataIntegrityViolationException e) {
            throw new AppException(BoardErrorCode.BOARD_REPORT_DUPLICATED);
        }
    }

    @Transactional(readOnly = true)
    public long getReportCountByStatus(BoardReportStatus status) {
        if (status == null) {
            return boardReportRepository.count();
        }
        return boardReportRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public boolean hasReported(Long boardId, Long reporterId) {
        if (boardId == null || reporterId == null) {
            return false;
        }
        return boardReportRepository.existsByBoardIdAndReporterIdAndStatus(boardId, reporterId, BoardReportStatus.OPEN);
    }

    @Transactional(readOnly = true)
    public Page<BoardReport> getReports(BoardReportStatus status, Pageable pageable) {
        if (status == null) {
            return boardReportRepository.findAllWithBoardAndReporter(pageable);
        }
        return boardReportRepository.findByStatusWithBoardAndReporter(status, pageable);
    }

    @Transactional
    public void updateReportStatus(Long reportId,
                                   BoardReportStatus status,
                                   Long processorId,
                                   String processNote) {
        if (reportId == null) {
            throw new IllegalArgumentException("신고 ID가 올바르지 않습니다.");
        }
        if (status == null) {
            throw new IllegalArgumentException("변경할 신고 상태가 올바르지 않습니다.");
        }

        BoardReport report = boardReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고 내역을 찾을 수 없습니다."));
        BoardReportStatus previousStatus = report.getStatus();

        Account processor = null;
        if (status != BoardReportStatus.OPEN) {
            if (processorId == null) {
                throw new IllegalArgumentException("처리자 정보가 없습니다.");
            }
            processor = accountRepository.findById(processorId)
                    .orElseThrow(() -> new IllegalArgumentException("처리자 정보를 찾을 수 없습니다."));
        }

        report.updateStatus(status, processor, boardReportTextSanitizer.sanitizeProcessNote(processNote), LocalDateTime.now());
        boardReportPolicy.syncBoardVisibility(report.getBoard());

        if (previousStatus != status && status != BoardReportStatus.OPEN) {
            notificationService.notifyOnReportProcessed(report);
        }
    }
}
