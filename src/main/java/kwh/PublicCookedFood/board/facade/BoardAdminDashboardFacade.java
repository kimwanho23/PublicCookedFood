package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.board.service.BoardReportService;
import kwh.PublicCookedFood.board.service.BoardService;
import kwh.PublicCookedFood.food.service.RecipeReviewService;
import kwh.PublicCookedFood.user.service.UserActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BoardAdminDashboardFacade {

    private final BoardReportService boardReportService;
    private final BoardService boardService;
    private final RecipeReviewService recipeReviewService;
    private final UserActivityLogService userActivityLogService;

    public BoardAdminFacade.DashboardViewData loadDashboardData() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);

        return new BoardAdminFacade.DashboardViewData(
                boardReportService.getReportCountByStatus(BoardReportStatus.OPEN),
                boardReportService.getReportCountByStatus(BoardReportStatus.RESOLVED),
                boardReportService.getReportCountByStatus(BoardReportStatus.REJECTED),
                boardReportService.getReportCountByStatus(null),
                boardService.getHiddenByReportCount(),
                boardService.getPopularBoardsSince(weekAgo, 8),
                recipeReviewService.getTopReviewRankings(monthAgo, 8),
                userActivityLogService.getRecentActivities(15)
        );
    }
}
