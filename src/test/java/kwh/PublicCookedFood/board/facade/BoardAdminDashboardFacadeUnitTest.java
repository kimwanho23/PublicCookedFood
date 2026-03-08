package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.repository.BoardReportRepository;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.board.service.BoardReportService;
import kwh.PublicCookedFood.board.service.BoardService;
import kwh.PublicCookedFood.food.repository.Recipe_INFO_Repository;
import kwh.PublicCookedFood.food.service.RecipeReviewService;
import kwh.PublicCookedFood.account.domain.AccountActivityLog;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.account.service.AccountActivityLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardAdminDashboardFacadeUnitTest {

    @Mock
    private BoardReportService boardReportService;

    @Mock
    private BoardService boardService;

    @Mock
    private RecipeReviewService recipeReviewService;

    @Mock
    private AccountActivityLogService accountActivityLogService;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private CommentsRepository commentsRepository;

    @Mock
    private BoardReportRepository boardReportRepository;

    @Mock
    private Recipe_INFO_Repository recipeInfoRepository;

    @Mock
    private AccountRepository accountRepository;

    private BoardAdminDashboardFacade boardAdminDashboardFacade;

    @BeforeEach
    void setUp() {
        boardAdminDashboardFacade = new BoardAdminDashboardFacade(
                boardReportService,
                boardService,
                recipeReviewService,
                accountActivityLogService,
                boardRepository,
                commentsRepository,
                boardReportRepository,
                recipeInfoRepository,
                accountRepository
        );
    }

    @Test
    void loadDashboardData_mapsRecentActivitiesToReadableTargets() {
        Account actor = account(1L, "actor");
        Account blockedAccount = account(2L, "blocked-account");
        Board board = Board.builder()
                .id(10L)
                .title("board title")
                .account(actor)
                .hiddenByReport(false)
                .build();
        Comments comment = Comments.builder()
                .id(30L)
                .account(actor)
                .board(board)
                .contents("comment\nbody")
                .build();

        AccountActivityLog commentActivity = AccountActivityLog.builder()
                .account(actor)
                .action("BOARD_COMMENT_CREATE")
                .detail("boardId=10,commentId=30,parentId=-")
                .build();
        AccountActivityLog blockActivity = AccountActivityLog.builder()
                .account(actor)
                .action("ACCOUNT_BLOCK_ADD")
                .detail("targetAccountId=2")
                .build();
        AccountActivityLog notificationActivity = AccountActivityLog.builder()
                .account(actor)
                .action("NOTIFICATION_SETTING_UPDATE")
                .detail("enabled=false")
                .build();

        when(boardReportService.getReportCountByStatus(BoardReportStatus.OPEN)).thenReturn(1L);
        when(boardReportService.getReportCountByStatus(BoardReportStatus.RESOLVED)).thenReturn(2L);
        when(boardReportService.getReportCountByStatus(BoardReportStatus.REJECTED)).thenReturn(3L);
        when(boardReportService.getReportCountByStatus(null)).thenReturn(6L);
        when(boardService.getHiddenByReportCount()).thenReturn(4L);
        when(boardService.getPopularBoardsSince(any(), eq(8))).thenReturn(List.of());
        when(recipeReviewService.getTopReviewRankings(any(), eq(8))).thenReturn(List.of());
        when(accountActivityLogService.getRecentActivities(15))
                .thenReturn(List.of(commentActivity, blockActivity, notificationActivity));
        when(boardRepository.findAllById(List.of(10L))).thenReturn(List.of(board));
        when(commentsRepository.findAllById(List.of(30L))).thenReturn(List.of(comment));
        when(accountRepository.findAllById(List.of(2L))).thenReturn(List.of(blockedAccount));

        BoardAdminFacade.DashboardViewData result = boardAdminDashboardFacade.loadDashboardData();

        assertThat(result.recentActivities()).hasSize(3);
        assertThat(result.recentActivities().get(0).accountId()).isEqualTo(1L);
        assertThat(result.recentActivities().get(0).accountName()).isEqualTo("actor");
        assertThat(result.recentActivities().get(0).action()).isEqualTo("BOARD_COMMENT_CREATE");
        assertThat(result.recentActivities().get(0).detail()).isEqualTo("board title / comment body");
        assertThat(result.recentActivities().get(1).detail()).isEqualTo("blocked-account");
        assertThat(result.recentActivities().get(2).detail()).isNull();
    }

    private Account account(Long accountId, String name) {
        return Account.builder()
                .id(accountId)
                .email(name + "@test.com")
                .name(name)
                .loginMethod("Current")
                .build();
    }
}

