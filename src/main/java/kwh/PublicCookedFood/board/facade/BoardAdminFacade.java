package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.domain.BoardReport;
import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.board.domain.BoardThumbnailDisplayMode;
import kwh.PublicCookedFood.board.dto.response.BoardPolicyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardAdminFacade {

    public static final int MIN_FEATURED_THRESHOLD = 1;
    public static final int MAX_FEATURED_THRESHOLD = 10000;
    public static final String REPORT_FILTER_ALL = "ALL";
    public static final String REPORT_FILTER_OPEN = "OPEN";

    private final BoardAdminDashboardFacade boardAdminDashboardFacade;
    private final BoardAdminPolicyFacade boardAdminPolicyFacade;
    private final BoardAdminReportFacade boardAdminReportFacade;

    public DashboardViewData loadDashboardData() {
        return boardAdminDashboardFacade.loadDashboardData();
    }

    public PolicyViewData loadPolicyData() {
        return boardAdminPolicyFacade.loadPolicyData();
    }

    public void updateBoardPolicy(Integer featuredLikeThreshold,
                                  BoardThumbnailDisplayMode thumbnailDisplayMode,
                                  Long actorAccountId) {
        boardAdminPolicyFacade.updateBoardPolicy(featuredLikeThreshold, thumbnailDisplayMode, actorAccountId);
    }

    public ReportPageViewData loadReportPage(String statusFilter, Pageable pageable) {
        return boardAdminReportFacade.loadReportPage(statusFilter, pageable);
    }

    public void updateReportStatus(Long reportId,
                                   BoardReportStatus status,
                                   String processNote,
                                   Long actorAccountId) {
        boardAdminReportFacade.updateReportStatus(reportId, status, processNote, actorAccountId);
    }

    public int normalizeFeaturedThreshold(Integer featuredLikeThreshold) {
        return boardAdminPolicyFacade.normalizeFeaturedThreshold(featuredLikeThreshold);
    }

    public String normalizeReportFilter(String statusFilter) {
        return boardAdminReportFacade.normalizeReportFilter(statusFilter);
    }

    public record DashboardRecentActivityView(LocalDateTime regTime,
                                              Long accountId,
                                              String accountName,
                                              String action,
                                              String detail) {
    }

    public record DashboardViewData(long openCount,
                                    long resolvedCount,
                                    long rejectedCount,
                                    long allCount,
                                    long hiddenByReportCount,
                                    List<?> popularBoards,
                                    List<?> reviewRankings,
                                    List<DashboardRecentActivityView> recentActivities) {
        public DashboardViewData {
            popularBoards = popularBoards == null ? List.of() : List.copyOf(popularBoards);
            reviewRankings = reviewRankings == null ? List.of() : List.copyOf(reviewRankings);
            recentActivities = recentActivities == null ? List.of() : List.copyOf(recentActivities);
        }
    }

    public record PolicyViewData(BoardPolicyResponse policy,
                                 int minFeaturedThreshold,
                                 int maxFeaturedThreshold) {
    }

    public record ReportPageViewData(Page<BoardReport> reportPage,
                                     String reportFilter,
                                     long openCount,
                                     long resolvedCount,
                                     long rejectedCount,
                                     long allCount,
                                     BoardReportStatus[] reportStatusValues) {
        public ReportPageViewData {
            reportStatusValues = reportStatusValues == null
                    ? new BoardReportStatus[0]
                    : reportStatusValues.clone();
        }

        @Override
        public BoardReportStatus[] reportStatusValues() {
            return reportStatusValues.clone();
        }
    }
}
