package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.domain.BoardReport;
import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.board.service.BoardReportService;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.CommonErrorCode;
import kwh.PublicCookedFood.account.audit.BoardAuditPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BoardAdminReportFacade {

    private final BoardReportService boardReportService;
    private final BoardAuditPublisher boardAuditPublisher;

    public BoardAdminFacade.ReportPageViewData loadReportPage(String statusFilter, Pageable pageable) {
        String normalizedFilter = normalizeReportFilter(statusFilter);
        BoardReportStatus selectedStatus = parseReportStatus(normalizedFilter);
        Page<BoardReport> reportPage = boardReportService.getReports(selectedStatus, pageable);

        return new BoardAdminFacade.ReportPageViewData(
                reportPage,
                normalizedFilter,
                boardReportService.getReportCountByStatus(BoardReportStatus.OPEN),
                boardReportService.getReportCountByStatus(BoardReportStatus.RESOLVED),
                boardReportService.getReportCountByStatus(BoardReportStatus.REJECTED),
                boardReportService.getReportCountByStatus(null),
                BoardReportStatus.values()
        );
    }

    @Transactional
    public void updateReportStatus(Long reportId,
                                   BoardReportStatus status,
                                   String processNote,
                                   Long actorAccountId) {
        if (actorAccountId == null) {
            throw new AppException(CommonErrorCode.ACCESS_DENIED, "신고 처리 권한이 없습니다.");
        }
        boardReportService.updateReportStatus(reportId, status, actorAccountId, processNote);
        boardAuditPublisher.boardReportStatusUpdate(actorAccountId, reportId, status.name());
    }

    public String normalizeReportFilter(String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank()) {
            return BoardAdminFacade.REPORT_FILTER_OPEN;
        }
        String normalized = statusFilter.trim().toUpperCase(Locale.ROOT);
        if (BoardAdminFacade.REPORT_FILTER_ALL.equals(normalized)) {
            return BoardAdminFacade.REPORT_FILTER_ALL;
        }
        try {
            BoardReportStatus.valueOf(normalized);
            return normalized;
        } catch (IllegalArgumentException ignored) {
            return BoardAdminFacade.REPORT_FILTER_OPEN;
        }
    }

    private BoardReportStatus parseReportStatus(String normalizedFilter) {
        if (BoardAdminFacade.REPORT_FILTER_ALL.equals(normalizedFilter)) {
            return null;
        }
        return BoardReportStatus.valueOf(normalizedFilter);
    }
}
