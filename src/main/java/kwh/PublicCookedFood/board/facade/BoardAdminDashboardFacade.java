package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.AccountActivityLog;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.account.service.AccountActivityLogService;
import kwh.PublicCookedFood.board.application.query.BoardCardViewAssembler;
import kwh.PublicCookedFood.board.application.query.view.BoardCardView;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardReport;
import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.repository.BoardReportRepository;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.board.service.query.BoardPopularityQueryService;
import kwh.PublicCookedFood.board.service.query.BoardReportQueryService;
import kwh.PublicCookedFood.board.service.support.BoardStatsSummary;
import kwh.PublicCookedFood.board.service.support.BoardStatsSummaryResolver;
import kwh.PublicCookedFood.food.dto.response.RecipeRankingResponse;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.food.repository.Recipe_INFO_Repository;
import kwh.PublicCookedFood.food.service.RecipeReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardAdminDashboardFacade {

    private static final int RECENT_ACTIVITY_LIMIT = 15;
    private static final int COMMENT_DETAIL_LIMIT = 48;

    private final BoardReportQueryService boardReportQueryService;
    private final BoardPopularityQueryService boardPopularityQueryService;
    private final RecipeReviewService recipeReviewService;
    private final AccountActivityLogService accountActivityLogService;
    private final BoardRepository boardRepository;
    private final CommentsRepository commentsRepository;
    private final BoardReportRepository boardReportRepository;
    private final Recipe_INFO_Repository recipeInfoRepository;
    private final AccountRepository accountRepository;
    private final BoardAdminActivityLogParser activityLogParser;
    private final BoardStatsSummaryResolver boardStatsSummaryResolver;
    private final BoardCardViewAssembler boardCardViewAssembler;

    @Transactional(readOnly = true)
    public DashboardViewData loadDashboardData() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);
        List<AccountActivityLog> recentActivities = accountActivityLogService.getRecentActivities(RECENT_ACTIVITY_LIMIT);
        List<Board> popularBoards = boardPopularityQueryService.getPopularBoardsSince(weekAgo, 8);
        Map<Long, BoardStatsSummary> boardStatsMap = boardStatsSummaryResolver.resolve(popularBoards);

        return new DashboardViewData(
                boardReportQueryService.getReportCountByStatus(BoardReportStatus.OPEN),
                boardReportQueryService.getReportCountByStatus(BoardReportStatus.RESOLVED),
                boardReportQueryService.getReportCountByStatus(BoardReportStatus.REJECTED),
                boardReportQueryService.getReportCountByStatus(null),
                boardRepository.countByHiddenByReportTrue(),
                boardCardViewAssembler.toList(popularBoards, boardStatsMap),
                recipeReviewService.getTopReviewRankings(monthAgo, 8),
                mapRecentActivities(recentActivities)
        );
    }

    private List<DashboardRecentActivityView> mapRecentActivities(List<AccountActivityLog> recentActivities) {
        if (recentActivities == null || recentActivities.isEmpty()) {
            return Collections.emptyList();
        }

        List<BoardAdminParsedActivity> parsedActivities = recentActivities.stream()
                .map(activityLogParser::parse)
                .collect(Collectors.toList());

        Map<Long, Board> boardsById = loadBoards(parsedActivities);
        Map<Long, Comments> commentsById = loadComments(parsedActivities);
        Map<Long, BoardReport> reportsById = loadReports(parsedActivities);
        Map<Long, Recipe_INFO> recipesById = loadRecipes(parsedActivities);
        Map<Long, Account> accountsById = loadAccounts(parsedActivities);

        return parsedActivities.stream()
                .map(activity -> toRecentActivityView(activity, boardsById, commentsById, reportsById, recipesById, accountsById))
                .collect(Collectors.toList());
    }

    private DashboardRecentActivityView toRecentActivityView(
            BoardAdminParsedActivity parsedActivity,
            Map<Long, Board> boardsById,
            Map<Long, Comments> commentsById,
            Map<Long, BoardReport> reportsById,
            Map<Long, Recipe_INFO> recipesById,
            Map<Long, Account> accountsById) {
        Optional<String> detailText = resolveDetail(parsedActivity, boardsById, commentsById, reportsById, recipesById, accountsById);
        DashboardRecentActivityDetailView detailView = detailText.isPresent()
                ? DashboardRecentActivityDetailView.from(detailText.get())
                : DashboardRecentActivityDetailView.empty();

        return new DashboardRecentActivityView(
                activityTime(parsedActivity.activity()),
                DashboardRecentActivityActorView.from(activityActor(parsedActivity.activity())),
                parsedActivity.action().rawValue(),
                detailView
        );
    }

    private Optional<String> resolveDetail(BoardAdminParsedActivity parsedActivity,
                                           Map<Long, Board> boardsById,
                                           Map<Long, Comments> commentsById,
                                           Map<Long, BoardReport> reportsById,
                                           Map<Long, Recipe_INFO> recipesById,
                                           Map<Long, Account> accountsById) {
        BoardAdminActivityAction action = parsedActivity.action();
        if (action == BoardAdminActivityAction.BOARD_CREATE
                || action == BoardAdminActivityAction.BOARD_UPDATE
                || action == BoardAdminActivityAction.BOARD_DELETE
                || action == BoardAdminActivityAction.BOARD_SCRAP_ADD
                || action == BoardAdminActivityAction.BOARD_SCRAP_REMOVE
                || action == BoardAdminActivityAction.BOARD_LIKE_TOGGLE
                || action == BoardAdminActivityAction.BOARD_REPORT_CREATE) {
            return resolveBoardTitle(parsedActivity.detail(), boardsById);
        }
        if (action == BoardAdminActivityAction.BOARD_COMMENT_CREATE
                || action == BoardAdminActivityAction.BOARD_COMMENT_DELETE) {
            return resolveCommentTarget(parsedActivity.detail(), boardsById, commentsById);
        }
        if (action == BoardAdminActivityAction.BOARD_REPORT_STATUS_UPDATE) {
            return resolveReportTarget(parsedActivity.detail(), reportsById);
        }
        if (action == BoardAdminActivityAction.BOOKMARK_ADD
                || action == BoardAdminActivityAction.BOOKMARK_REMOVE
                || action == BoardAdminActivityAction.RECIPE_REVIEW_UPSERT) {
            return resolveRecipeName(parsedActivity.detail(), recipesById);
        }
        if (action == BoardAdminActivityAction.ACCOUNT_BLOCK_ADD
                || action == BoardAdminActivityAction.ACCOUNT_BLOCK_REMOVE) {
            return resolveAccountName(parsedActivity.detail(), accountsById);
        }
        return Optional.empty();
    }

    private Map<Long, Board> loadBoards(List<BoardAdminParsedActivity> parsedActivities) {
        List<Long> boardIds = collectDistinctBoardIds(parsedActivities);

        if (boardIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return boardRepository.findAllById(boardIds).stream()
                .collect(Collectors.toMap(Board::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, Comments> loadComments(List<BoardAdminParsedActivity> parsedActivities) {
        List<Long> commentIds = collectDistinctCommentIds(parsedActivities);

        if (commentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return commentsRepository.findAllById(commentIds).stream()
                .collect(Collectors.toMap(Comments::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, BoardReport> loadReports(List<BoardAdminParsedActivity> parsedActivities) {
        List<Long> reportIds = collectDistinctReportIds(parsedActivities);

        if (reportIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return boardReportRepository.findAllById(reportIds).stream()
                .collect(Collectors.toMap(BoardReport::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, Recipe_INFO> loadRecipes(List<BoardAdminParsedActivity> parsedActivities) {
        List<Long> recipeIds = collectDistinctRecipeIds(parsedActivities);

        if (recipeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return recipeInfoRepository.findAllByRecipeIDIn(recipeIds).stream()
                .collect(Collectors.toMap(Recipe_INFO::getRecipeID, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, Account> loadAccounts(List<BoardAdminParsedActivity> parsedActivities) {
        List<Long> accountIds = collectDistinctTargetAccountIds(parsedActivities);

        if (accountIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return accountRepository.findAllById(accountIds).stream()
                .collect(Collectors.toMap(Account::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private List<Long> collectDistinctBoardIds(List<BoardAdminParsedActivity> parsedActivities) {
        Map<Long, Boolean> uniqueIds = new LinkedHashMap<Long, Boolean>();
        for (BoardAdminParsedActivity parsedActivity : parsedActivities) {
            Optional<Long> boardId = parsedActivity.detail().boardId();
            if (boardId.isPresent()) {
                uniqueIds.put(boardId.get(), Boolean.TRUE);
            }
        }
        return new ArrayList<Long>(uniqueIds.keySet());
    }

    private List<Long> collectDistinctCommentIds(List<BoardAdminParsedActivity> parsedActivities) {
        Map<Long, Boolean> uniqueIds = new LinkedHashMap<Long, Boolean>();
        for (BoardAdminParsedActivity parsedActivity : parsedActivities) {
            Optional<Long> commentId = parsedActivity.detail().commentId();
            if (commentId.isPresent()) {
                uniqueIds.put(commentId.get(), Boolean.TRUE);
            }
        }
        return new ArrayList<Long>(uniqueIds.keySet());
    }

    private List<Long> collectDistinctReportIds(List<BoardAdminParsedActivity> parsedActivities) {
        Map<Long, Boolean> uniqueIds = new LinkedHashMap<Long, Boolean>();
        for (BoardAdminParsedActivity parsedActivity : parsedActivities) {
            Optional<Long> reportId = parsedActivity.detail().reportId();
            if (reportId.isPresent()) {
                uniqueIds.put(reportId.get(), Boolean.TRUE);
            }
        }
        return new ArrayList<Long>(uniqueIds.keySet());
    }

    private List<Long> collectDistinctRecipeIds(List<BoardAdminParsedActivity> parsedActivities) {
        Map<Long, Boolean> uniqueIds = new LinkedHashMap<Long, Boolean>();
        for (BoardAdminParsedActivity parsedActivity : parsedActivities) {
            Optional<Long> recipeId = parsedActivity.detail().recipeId();
            if (recipeId.isPresent()) {
                uniqueIds.put(recipeId.get(), Boolean.TRUE);
            }
        }
        return new ArrayList<Long>(uniqueIds.keySet());
    }

    private List<Long> collectDistinctTargetAccountIds(List<BoardAdminParsedActivity> parsedActivities) {
        Map<Long, Boolean> uniqueIds = new LinkedHashMap<Long, Boolean>();
        for (BoardAdminParsedActivity parsedActivity : parsedActivities) {
            Optional<Long> accountId = parsedActivity.detail().targetAccountId();
            if (accountId.isPresent()) {
                uniqueIds.put(accountId.get(), Boolean.TRUE);
            }
        }
        return new ArrayList<Long>(uniqueIds.keySet());
    }

    private Optional<String> resolveBoardTitle(BoardAdminActivityDetail detail, Map<Long, Board> boardsById) {
        return detail.boardId()
                .map(boardsById::get)
                .map(Board::getTitle)
                .flatMap(this::normalizeInline);
    }

    private Optional<String> resolveCommentTarget(BoardAdminActivityDetail detail,
                                                  Map<Long, Board> boardsById,
                                                  Map<Long, Comments> commentsById) {
        Optional<Comments> comment = detail.commentId().map(commentsById::get);

        Optional<String> boardTitle = resolveBoardTitle(detail, boardsById);
        if (!boardTitle.isPresent() && comment.isPresent()) {
            boardTitle = Optional.ofNullable(comment.get().getBoard())
                    .map(Board::getTitle)
                    .flatMap(this::normalizeInline);
        }

        Optional<String> commentContents = Optional.empty();
        if (comment.isPresent()) {
            commentContents = Optional.ofNullable(comment.get().getContents())
                    .flatMap(this::normalizeInline)
                    .map(this::shorten);
        }

        if (boardTitle.isPresent() && commentContents.isPresent()) {
            return Optional.of(boardTitle.get() + " / " + commentContents.get());
        }
        return boardTitle.isPresent() ? boardTitle : commentContents;
    }

    private Optional<String> resolveReportTarget(BoardAdminActivityDetail detail, Map<Long, BoardReport> reportsById) {
        return detail.reportId()
                .map(reportsById::get)
                .map(BoardReport::getBoard)
                .map(Board::getTitle)
                .flatMap(this::normalizeInline);
    }

    private Optional<String> resolveRecipeName(BoardAdminActivityDetail detail, Map<Long, Recipe_INFO> recipesById) {
        return detail.recipeId()
                .map(recipesById::get)
                .map(Recipe_INFO::getRecipeNMKO)
                .flatMap(this::normalizeInline);
    }

    private Optional<String> resolveAccountName(BoardAdminActivityDetail detail, Map<Long, Account> accountsById) {
        return detail.targetAccountId()
                .map(accountsById::get)
                .map(Account::getName)
                .flatMap(this::normalizeInline);
    }

    private String shorten(String rawValue) {
        if (rawValue.length() <= COMMENT_DETAIL_LIMIT) {
            return rawValue;
        }
        return rawValue.substring(0, Math.max(0, COMMENT_DETAIL_LIMIT - 3)) + "...";
    }

    private Optional<String> normalizeInline(String rawValue) {
        if (rawValue == null) {
            return Optional.empty();
        }

        String normalized = rawValue.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(normalized);
    }

    private LocalDateTime activityTime(AccountActivityLog activity) {
        return activity == null ? null : activity.getRegTime();
    }

    private Account activityActor(AccountActivityLog activity) {
        return activity == null ? null : activity.getAccount();
    }

    public static final class DashboardRecentActivityView {

        private final LocalDateTime regTime;
        private final DashboardRecentActivityActorView actor;
        private final String action;
        private final DashboardRecentActivityDetailView detailView;

        public DashboardRecentActivityView(LocalDateTime regTime,
                                           DashboardRecentActivityActorView actor,
                                           String action,
                                           DashboardRecentActivityDetailView detailView) {
            this.regTime = regTime;
            this.actor = actor == null ? DashboardRecentActivityActorView.anonymous() : actor;
            this.action = action;
            this.detailView = detailView == null ? DashboardRecentActivityDetailView.empty() : detailView;
        }

        public LocalDateTime regTime() {
            return regTime;
        }

        public LocalDateTime getRegTime() {
            return regTime;
        }

        public DashboardRecentActivityActorView actor() {
            return actor;
        }

        public DashboardRecentActivityActorView getActor() {
            return actor;
        }

        public String action() {
            return action;
        }

        public String getAction() {
            return action;
        }

        public DashboardRecentActivityDetailView detailView() {
            return detailView;
        }

        public DashboardRecentActivityDetailView getDetailView() {
            return detailView;
        }

        public Long accountId() {
            return actor.accountId();
        }

        public Long getAccountId() {
            return accountId();
        }

        public String accountName() {
            return actor.accountName();
        }

        public String getAccountName() {
            return accountName();
        }

        public String detail() {
            return detailView.text();
        }

        public String getDetail() {
            return detail();
        }
    }

    public static final class DashboardRecentActivityActorView {

        private final Long accountId;
        private final String accountName;

        public DashboardRecentActivityActorView(Long accountId, String accountName) {
            this.accountId = accountId;
            this.accountName = accountName;
        }

        public static DashboardRecentActivityActorView anonymous() {
            return new DashboardRecentActivityActorView(null, null);
        }

        public static DashboardRecentActivityActorView from(Account actor) {
            if (actor == null) {
                return anonymous();
            }
            return new DashboardRecentActivityActorView(actor.getId(), actor.getName());
        }

        public Long accountId() {
            return accountId;
        }

        public Long getAccountId() {
            return accountId;
        }

        public String accountName() {
            return accountName;
        }

        public String getAccountName() {
            return accountName;
        }
    }

    public static final class DashboardRecentActivityDetailView {

        private final String text;

        public DashboardRecentActivityDetailView(String text) {
            this.text = text;
        }

        public static DashboardRecentActivityDetailView empty() {
            return new DashboardRecentActivityDetailView(null);
        }

        public static DashboardRecentActivityDetailView from(String text) {
            if (text == null || text.trim().isEmpty()) {
                return empty();
            }
            return new DashboardRecentActivityDetailView(text);
        }

        public String text() {
            return text;
        }

        public String getText() {
            return text;
        }
    }

    public static final class DashboardViewData {

        private final long openCount;
        private final long resolvedCount;
        private final long rejectedCount;
        private final long allCount;
        private final long hiddenByReportCount;
        private final List<BoardCardView> popularBoards;
        private final List<RecipeRankingResponse> reviewRankings;
        private final List<DashboardRecentActivityView> recentActivities;

        public DashboardViewData(long openCount,
                                 long resolvedCount,
                                 long rejectedCount,
                                 long allCount,
                                 long hiddenByReportCount,
                                 List<BoardCardView> popularBoards,
                                 List<RecipeRankingResponse> reviewRankings,
                                 List<DashboardRecentActivityView> recentActivities) {
            this.openCount = openCount;
            this.resolvedCount = resolvedCount;
            this.rejectedCount = rejectedCount;
            this.allCount = allCount;
            this.hiddenByReportCount = hiddenByReportCount;
            this.popularBoards = immutableList(popularBoards);
            this.reviewRankings = immutableList(reviewRankings);
            this.recentActivities = immutableList(recentActivities);
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

        public long hiddenByReportCount() {
            return hiddenByReportCount;
        }

        public long getHiddenByReportCount() {
            return hiddenByReportCount;
        }

        public List<BoardCardView> popularBoards() {
            return popularBoards;
        }

        public List<BoardCardView> getPopularBoards() {
            return popularBoards;
        }

        public List<RecipeRankingResponse> reviewRankings() {
            return reviewRankings;
        }

        public List<RecipeRankingResponse> getReviewRankings() {
            return reviewRankings;
        }

        public List<DashboardRecentActivityView> recentActivities() {
            return recentActivities;
        }

        public List<DashboardRecentActivityView> getRecentActivities() {
            return recentActivities;
        }

        private static <T> List<T> immutableList(List<T> source) {
            if (source == null || source.isEmpty()) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableList(new ArrayList<T>(source));
        }
    }
}
