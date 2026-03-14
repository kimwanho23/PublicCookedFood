package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.domain.BoardReport;
import kwh.PublicCookedFood.board.service.command.BoardReportCommandService;
import kwh.PublicCookedFood.board.service.command.BoardReportStatusUpdateCommand;
import kwh.PublicCookedFood.board.service.query.BoardReportQueryService;
import kwh.PublicCookedFood.board.service.query.BoardReportFilter;
import kwh.PublicCookedFood.board.service.query.BoardReportPageQuery;
import kwh.PublicCookedFood.account.audit.BoardAuditPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BoardAdminReportFacade {

    private final BoardReportQueryService boardReportQueryService;
    private final BoardReportCommandService boardReportCommandService;
    private final BoardAuditPublisher boardAuditPublisher;

    public ReportPageViewData loadReportPage(BoardReportPageQuery query) {
        BoardReportFilter filter = query.filter();
        Pageable pageable = query.pageable();
        Page<BoardReport> reportPage = filter.includesAllStatuses()
                ? boardReportQueryService.getReports(null, pageable)
                : boardReportQueryService.getReports(filter.status(), pageable);

        return new ReportPageViewData(
                reportPage,
                filter.paramValue(),
                boardReportQueryService.getReportCountByStatus(kwh.PublicCookedFood.board.domain.BoardReportStatus.OPEN),
                boardReportQueryService.getReportCountByStatus(kwh.PublicCookedFood.board.domain.BoardReportStatus.RESOLVED),
                boardReportQueryService.getReportCountByStatus(kwh.PublicCookedFood.board.domain.BoardReportStatus.REJECTED),
                boardReportQueryService.getReportCountByStatus(null),
                kwh.PublicCookedFood.board.domain.BoardReportStatus.values()
        );
    }

    @Transactional
    public void updateReportStatus(BoardReportStatusUpdateCommand command) {
        boardReportCommandService.updateReportStatus(command);
        boardAuditPublisher.boardReportStatusUpdate(command.processorId(), command.reportId(), command.status().name());
    }

    public static final class ReportPageViewData {

        private final Page<BoardReport> reportPage;
        private final String reportFilter;
        private final long openCount;
        private final long resolvedCount;
        private final long rejectedCount;
        private final long allCount;
        private final kwh.PublicCookedFood.board.domain.BoardReportStatus[] reportStatusValues;

        public ReportPageViewData(Page<BoardReport> reportPage,
                                  String reportFilter,
                                  long openCount,
                                  long resolvedCount,
                                  long rejectedCount,
                                  long allCount,
                                  kwh.PublicCookedFood.board.domain.BoardReportStatus[] reportStatusValues) {
            this.reportPage = Objects.requireNonNull(reportPage, "reportPage");
            this.reportFilter = reportFilter;
            this.openCount = openCount;
            this.resolvedCount = resolvedCount;
            this.rejectedCount = rejectedCount;
            this.allCount = allCount;
            this.reportStatusValues = reportStatusValues == null
                    ? new kwh.PublicCookedFood.board.domain.BoardReportStatus[0]
                    : reportStatusValues.clone();
        }

        public Page<BoardReport> reportPage() {
            return reportPage;
        }

        public Page<BoardReport> getReportPage() {
            return reportPage;
        }

        public String reportFilter() {
            return reportFilter;
        }

        public String getReportFilter() {
            return reportFilter;
        }

        public long openCount() {
            return openCount;
        }

        public long getOpenCount() {
            return openCount;
        }

        public long resolvedCount() {
            return resolvedCount;
        }

        public long getResolvedCount() {
            return resolvedCount;
        }

        public long rejectedCount() {
            return rejectedCount;
        }

        public long getRejectedCount() {
            return rejectedCount;
        }

        public long allCount() {
            return allCount;
        }

        public long getAllCount() {
            return allCount;
        }

        public kwh.PublicCookedFood.board.domain.BoardReportStatus[] reportStatusValues() {
            return reportStatusValues.clone();
        }

        public kwh.PublicCookedFood.board.domain.BoardReportStatus[] getReportStatusValues() {
            return reportStatusValues();
        }
    }

}
