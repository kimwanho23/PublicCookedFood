package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.application.query.BoardCardViewAssembler;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.repository.BoardReportRepository;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.board.service.query.BoardPopularityQueryService;
import kwh.PublicCookedFood.board.service.query.BoardReportQueryService;
import kwh.PublicCookedFood.board.service.support.BoardStatsSummaryResolver;
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
    private BoardReportQueryService boardReportQueryService;

    @Mock
    private BoardPopularityQueryService boardPopularityQueryService;

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
    @Mock
    private BoardStatsSummaryResolver boardStatsSummaryResolver;

    private BoardAdminDashboardFacade boardAdminDashboardFacade;

    @BeforeEach
    void setUp() {
        boardAdminDashboardFacade = new BoardAdminDashboardFacade(
                boardReportQueryService,
                boardPopularityQueryService,
                recipeReviewService,
                accountActivityLogService,
                boardRepository,
                commentsRepository,
                boardReportRepository,
                recipeInfoRepository,
                accountRepository,
                new BoardAdminActivityLogParser(),
                boardStatsSummaryResolver,
                new BoardCardViewAssembler()
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
        Board popularBoard = Board.builder()
                .id(11L)
                .title("popular board")
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

        when(boardReportQueryService.getReportCountByStatus(BoardReportStatus.OPEN)).thenReturn(1L);
        when(boardReportQueryService.getReportCountByStatus(BoardReportStatus.RESOLVED)).thenReturn(2L);
        when(boardReportQueryService.getReportCountByStatus(BoardReportStatus.REJECTED)).thenReturn(3L);
        when(boardReportQueryService.getReportCountByStatus(null)).thenReturn(6L);
        when(boardRepository.countByHiddenByReportTrue()).thenReturn(4L);
        when(boardPopularityQueryService.getPopularBoardsSince(any(), eq(8))).thenReturn(List.of(popularBoard));
        when(boardStatsSummaryResolver.resolve(List.of(popularBoard)))
                .thenReturn(java.util.Map.of(11L, new kwh.PublicCookedFood.board.service.support.BoardStatsSummary(8L, 6L, 4L)));
        when(recipeReviewService.getTopReviewRankings(any(), eq(8))).thenReturn(List.of());
        when(accountActivityLogService.getRecentActivities(15))
                .thenReturn(List.of(commentActivity, blockActivity, notificationActivity));
        when(boardRepository.findAllById(List.of(10L))).thenReturn(List.of(board));
        when(commentsRepository.findAllById(List.of(30L))).thenReturn(List.of(comment));
        when(accountRepository.findAllById(List.of(2L))).thenReturn(List.of(blockedAccount));

        BoardAdminDashboardFacade.DashboardViewData result = boardAdminDashboardFacade.loadDashboardData();

        assertThat(result.popularBoards()).hasSize(1);
        assertThat(result.popularBoards().get(0).boardId()).isEqualTo(11L);
        assertThat(result.popularBoards().get(0).likes()).isEqualTo(6L);
        assertThat(result.recentActivities()).hasSize(3);
        assertThat(result.recentActivities().get(0).actor().accountId()).isEqualTo(1L);
        assertThat(result.recentActivities().get(0).actor().accountName()).isEqualTo("actor");
        assertThat(result.recentActivities().get(0).accountId()).isEqualTo(1L);
        assertThat(result.recentActivities().get(0).accountName()).isEqualTo("actor");
        assertThat(result.recentActivities().get(0).action()).isEqualTo("BOARD_COMMENT_CREATE");
        assertThat(result.recentActivities().get(0).detailView().text()).isEqualTo("board title / comment body");
        assertThat(result.recentActivities().get(0).detail()).isEqualTo("board title / comment body");
        assertThat(result.recentActivities().get(1).detail()).isEqualTo("blocked-account");
        assertThat(result.recentActivities().get(2).detailView().text()).isNull();
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

