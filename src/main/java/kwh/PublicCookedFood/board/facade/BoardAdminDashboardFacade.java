package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardReport;
import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.repository.BoardReportRepository;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.board.service.BoardReportService;
import kwh.PublicCookedFood.board.service.BoardService;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.food.repository.Recipe_INFO_Repository;
import kwh.PublicCookedFood.food.service.RecipeReviewService;
import kwh.PublicCookedFood.account.domain.AccountActivityLog;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.account.service.AccountActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class BoardAdminDashboardFacade {

    private static final int RECENT_ACTIVITY_LIMIT = 15;
    private static final int COMMENT_DETAIL_LIMIT = 48;

    private final BoardReportService boardReportService;
    private final BoardService boardService;
    private final RecipeReviewService recipeReviewService;
    private final AccountActivityLogService accountActivityLogService;
    private final BoardRepository boardRepository;
    private final CommentsRepository commentsRepository;
    private final BoardReportRepository boardReportRepository;
    private final Recipe_INFO_Repository recipeInfoRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public BoardAdminFacade.DashboardViewData loadDashboardData() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);
        List<AccountActivityLog> recentActivities = accountActivityLogService.getRecentActivities(RECENT_ACTIVITY_LIMIT);

        return new BoardAdminFacade.DashboardViewData(
                boardReportService.getReportCountByStatus(BoardReportStatus.OPEN),
                boardReportService.getReportCountByStatus(BoardReportStatus.RESOLVED),
                boardReportService.getReportCountByStatus(BoardReportStatus.REJECTED),
                boardReportService.getReportCountByStatus(null),
                boardService.getHiddenByReportCount(),
                boardService.getPopularBoardsSince(weekAgo, 8),
                recipeReviewService.getTopReviewRankings(monthAgo, 8),
                mapRecentActivities(recentActivities)
        );
    }

    private List<BoardAdminFacade.DashboardRecentActivityView> mapRecentActivities(List<AccountActivityLog> recentActivities) {
        if (recentActivities == null || recentActivities.isEmpty()) {
            return List.of();
        }

        List<ParsedActivity> parsedActivities = recentActivities.stream()
                .map(activity -> new ParsedActivity(activity, parseDetailMap(activity.getDetail())))
                .toList();

        Map<Long, Board> boardsById = loadBoards(parsedActivities);
        Map<Long, Comments> commentsById = loadComments(parsedActivities);
        Map<Long, BoardReport> reportsById = loadReports(parsedActivities);
        Map<Long, Recipe_INFO> recipesById = loadRecipes(parsedActivities);
        Map<Long, Account> accountsById = loadAccounts(parsedActivities);

        return parsedActivities.stream()
                .map(activity -> toRecentActivityView(activity, boardsById, commentsById, reportsById, recipesById, accountsById))
                .toList();
    }

    private BoardAdminFacade.DashboardRecentActivityView toRecentActivityView(
            ParsedActivity parsedActivity,
            Map<Long, Board> boardsById,
            Map<Long, Comments> commentsById,
            Map<Long, BoardReport> reportsById,
            Map<Long, Recipe_INFO> recipesById,
            Map<Long, Account> accountsById) {
        AccountActivityLog activity = parsedActivity.activity();
        Account actor = activity.getAccount();

        return new BoardAdminFacade.DashboardRecentActivityView(
                activity.getRegTime(),
                actor == null ? null : actor.getId(),
                actor == null ? null : actor.getName(),
                activity.getAction(),
                resolveDetail(parsedActivity, boardsById, commentsById, reportsById, recipesById, accountsById)
        );
    }

    private String resolveDetail(ParsedActivity parsedActivity,
                                 Map<Long, Board> boardsById,
                                 Map<Long, Comments> commentsById,
                                 Map<Long, BoardReport> reportsById,
                                 Map<Long, Recipe_INFO> recipesById,
                                 Map<Long, Account> accountsById) {
        return switch (parsedActivity.activity().getAction()) {
            case "BOARD_CREATE",
                 "BOARD_UPDATE",
                 "BOARD_DELETE",
                 "BOARD_SCRAP_ADD",
                 "BOARD_SCRAP_REMOVE",
                 "BOARD_LIKE_TOGGLE",
                 "BOARD_REPORT_CREATE" -> resolveBoardTitle(parsedActivity.detailMap(), boardsById);
            case "BOARD_COMMENT_CREATE",
                 "BOARD_COMMENT_DELETE" -> resolveCommentTarget(parsedActivity.detailMap(), boardsById, commentsById);
            case "BOARD_REPORT_STATUS_UPDATE" -> resolveReportTarget(parsedActivity.detailMap(), reportsById);
            case "BOOKMARK_ADD",
                 "BOOKMARK_REMOVE",
                 "RECIPE_REVIEW_UPSERT" -> resolveRecipeName(parsedActivity.detailMap(), recipesById);
            case "ACCOUNT_BLOCK_ADD",
                 "ACCOUNT_BLOCK_REMOVE" -> resolveAccountName(parsedActivity.detailMap(), accountsById);
            default -> null;
        };
    }

    private Map<Long, Board> loadBoards(List<ParsedActivity> parsedActivities) {
        List<Long> boardIds = parsedActivities.stream()
                .map(parsedActivity -> parseLong(parsedActivity.detailMap(), "boardId"))
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();

        if (boardIds.isEmpty()) {
            return Map.of();
        }

        return boardRepository.findAllById(boardIds).stream()
                .collect(java.util.stream.Collectors.toMap(Board::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, Comments> loadComments(List<ParsedActivity> parsedActivities) {
        List<Long> commentIds = parsedActivities.stream()
                .map(parsedActivity -> parseLong(parsedActivity.detailMap(), "commentId"))
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();

        if (commentIds.isEmpty()) {
            return Map.of();
        }

        return commentsRepository.findAllById(commentIds).stream()
                .collect(java.util.stream.Collectors.toMap(Comments::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, BoardReport> loadReports(List<ParsedActivity> parsedActivities) {
        List<Long> reportIds = parsedActivities.stream()
                .map(parsedActivity -> parseLong(parsedActivity.detailMap(), "reportId"))
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();

        if (reportIds.isEmpty()) {
            return Map.of();
        }

        return boardReportRepository.findAllById(reportIds).stream()
                .collect(java.util.stream.Collectors.toMap(BoardReport::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, Recipe_INFO> loadRecipes(List<ParsedActivity> parsedActivities) {
        List<Long> recipeIds = parsedActivities.stream()
                .map(parsedActivity -> parseLong(parsedActivity.detailMap(), "recipeId"))
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();

        if (recipeIds.isEmpty()) {
            return Map.of();
        }

        return recipeInfoRepository.findAllByRecipeIDIn(recipeIds).stream()
                .collect(java.util.stream.Collectors.toMap(Recipe_INFO::getRecipeID, Function.identity(), (left, right) -> left));
    }

    private Map<Long, Account> loadAccounts(List<ParsedActivity> parsedActivities) {
        List<Long> accountIds = parsedActivities.stream()
                .map(parsedActivity -> parseActivityTargetAccountId(parsedActivity.detailMap()))
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();

        if (accountIds.isEmpty()) {
            return Map.of();
        }

        return accountRepository.findAllById(accountIds).stream()
                .collect(java.util.stream.Collectors.toMap(Account::getId, Function.identity(), (left, right) -> left));
    }

    private String resolveBoardTitle(Map<String, String> detailMap, Map<Long, Board> boardsById) {
        Board board = boardsById.get(parseLong(detailMap, "boardId"));
        return board == null ? null : normalizeInline(board.getTitle());
    }

    private String resolveCommentTarget(Map<String, String> detailMap,
                                        Map<Long, Board> boardsById,
                                        Map<Long, Comments> commentsById) {
        Comments comment = commentsById.get(parseLong(detailMap, "commentId"));
        String boardTitle = resolveBoardTitle(detailMap, boardsById);
        if (boardTitle == null && comment != null && comment.getBoard() != null) {
            boardTitle = normalizeInline(comment.getBoard().getTitle());
        }

        String commentContents = comment == null ? null : shorten(normalizeInline(comment.getContents()), COMMENT_DETAIL_LIMIT);
        if (boardTitle != null && commentContents != null) {
            return boardTitle + " / " + commentContents;
        }
        if (boardTitle != null) {
            return boardTitle;
        }
        return commentContents;
    }

    private String resolveReportTarget(Map<String, String> detailMap, Map<Long, BoardReport> reportsById) {
        BoardReport report = reportsById.get(parseLong(detailMap, "reportId"));
        if (report == null || report.getBoard() == null) {
            return null;
        }
        return normalizeInline(report.getBoard().getTitle());
    }

    private String resolveRecipeName(Map<String, String> detailMap, Map<Long, Recipe_INFO> recipesById) {
        Recipe_INFO recipe = recipesById.get(parseLong(detailMap, "recipeId"));
        return recipe == null ? null : normalizeInline(recipe.getRecipeNMKO());
    }

    private String resolveAccountName(Map<String, String> detailMap, Map<Long, Account> accountsById) {
        Account account = accountsById.get(parseActivityTargetAccountId(detailMap));
        return account == null ? null : normalizeInline(account.getName());
    }

    private Long parseActivityTargetAccountId(Map<String, String> detailMap) {
        return parseLong(detailMap, "targetAccountId");
    }

    private Map<String, String> parseDetailMap(String rawDetail) {
        if (rawDetail == null || rawDetail.isBlank()) {
            return Map.of();
        }

        Map<String, String> detailMap = new LinkedHashMap<>();
        for (String token : rawDetail.split(",")) {
            int delimiterIndex = token.indexOf('=');
            if (delimiterIndex <= 0) {
                continue;
            }

            String key = token.substring(0, delimiterIndex).trim();
            String value = token.substring(delimiterIndex + 1).trim();
            if (!key.isEmpty() && !value.isEmpty()) {
                detailMap.put(key, value);
            }
        }
        return detailMap;
    }

    private Long parseLong(Map<String, String> detailMap, String key) {
        String rawValue = detailMap.get(key);
        if (rawValue == null || rawValue.isBlank() || "-".equals(rawValue)) {
            return null;
        }

        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String normalizeInline(String rawValue) {
        if (rawValue == null) {
            return null;
        }

        String normalized = rawValue.replaceAll("\\s+", " ").trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String shorten(String rawValue, int maxLength) {
        if (rawValue == null || rawValue.length() <= maxLength) {
            return rawValue;
        }
        return rawValue.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private record ParsedActivity(AccountActivityLog activity, Map<String, String> detailMap) {
    }
}
