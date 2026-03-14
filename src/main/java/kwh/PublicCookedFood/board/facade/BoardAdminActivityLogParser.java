package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.account.domain.AccountActivityLog;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class BoardAdminActivityLogParser {

    public BoardAdminParsedActivity parse(AccountActivityLog activity) {
        if (activity == null) {
            return new BoardAdminParsedActivity(null, BoardAdminActivityAction.UNKNOWN, BoardAdminActivityDetail.empty());
        }
        return new BoardAdminParsedActivity(
                activity,
                BoardAdminActivityAction.from(activity.getAction()),
                parseDetail(activity.getDetail())
        );
    }

    private BoardAdminActivityDetail parseDetail(String rawDetail) {
        if (rawDetail == null || rawDetail.trim().isEmpty()) {
            return BoardAdminActivityDetail.empty();
        }

        BoardAdminActivityDetailBuilder builder = new BoardAdminActivityDetailBuilder();
        for (BoardAdminActivityDetailToken token : BoardAdminActivityRawDetail.parse(rawDetail).tokens()) {
            builder.assign(token);
        }
        return builder.build();
    }

    private static final class BoardAdminActivityDetailBuilder {
        private Long boardId;
        private Long commentId;
        private Long reportId;
        private Long recipeId;
        private Long targetAccountId;

        private void assign(BoardAdminActivityDetailToken token) {
            switch (token.key()) {
                case BOARD_ID:
                    boardId = token.value();
                    break;
                case COMMENT_ID:
                    commentId = token.value();
                    break;
                case REPORT_ID:
                    reportId = token.value();
                    break;
                case RECIPE_ID:
                    recipeId = token.value();
                    break;
                case TARGET_ACCOUNT_ID:
                    targetAccountId = token.value();
                    break;
                default:
                    break;
            }
        }

        private BoardAdminActivityDetail build() {
            return new BoardAdminActivityDetail(
                    boardId,
                    commentId,
                    reportId,
                    recipeId,
                    targetAccountId
            );
        }
    }
}

final class BoardAdminActivityRawDetail {

    private final List<BoardAdminActivityDetailToken> tokens;

    BoardAdminActivityRawDetail(List<BoardAdminActivityDetailToken> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            this.tokens = Collections.emptyList();
        } else {
            this.tokens = Collections.unmodifiableList(new ArrayList<BoardAdminActivityDetailToken>(tokens));
        }
    }

    static BoardAdminActivityRawDetail parse(String rawDetail) {
        if (rawDetail == null || rawDetail.trim().isEmpty()) {
            return new BoardAdminActivityRawDetail(Collections.<BoardAdminActivityDetailToken>emptyList());
        }
        String[] rawTokens = rawDetail.split(",");
        List<BoardAdminActivityDetailToken> tokens = new ArrayList<BoardAdminActivityDetailToken>();
        for (String rawToken : rawTokens) {
            Optional<BoardAdminActivityDetailToken> parsedToken = BoardAdminActivityDetailToken.fromRaw(rawToken);
            if (parsedToken.isPresent()) {
                tokens.add(parsedToken.get());
            }
        }
        return new BoardAdminActivityRawDetail(tokens);
    }

    List<BoardAdminActivityDetailToken> tokens() {
        return tokens;
    }
}

final class BoardAdminActivityDetailToken {

    private final BoardAdminActivityDetailKey key;
    private final long value;

    BoardAdminActivityDetailToken(BoardAdminActivityDetailKey key, long value) {
        this.key = Objects.requireNonNull(key, "key");
        this.value = value;
    }

    static Optional<BoardAdminActivityDetailToken> fromRaw(String rawToken) {
        if (rawToken == null || rawToken.trim().isEmpty()) {
            return Optional.empty();
        }
        int delimiterIndex = rawToken.indexOf('=');
        if (delimiterIndex <= 0) {
            return Optional.empty();
        }

        String rawKey = rawToken.substring(0, delimiterIndex).trim();
        String rawValue = rawToken.substring(delimiterIndex + 1).trim();
        if (rawKey.isEmpty() || rawValue.isEmpty()) {
            return Optional.empty();
        }

        Long parsedValue = parsePositiveLong(rawValue);
        if (parsedValue == null) {
            return Optional.empty();
        }

        Optional<BoardAdminActivityDetailKey> detailKey = BoardAdminActivityDetailKey.from(rawKey);
        if (!detailKey.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(new BoardAdminActivityDetailToken(detailKey.get(), parsedValue.longValue()));
    }

    private static Long parsePositiveLong(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty() || "-".equals(rawValue)) {
            return null;
        }
        try {
            long parsedValue = Long.parseLong(rawValue);
            if (parsedValue <= 0) {
                return null;
            }
            return parsedValue;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    BoardAdminActivityDetailKey key() {
        return key;
    }

    long value() {
        return value;
    }
}

enum BoardAdminActivityDetailKey {
    BOARD_ID("boardId"),
    COMMENT_ID("commentId"),
    REPORT_ID("reportId"),
    RECIPE_ID("recipeId"),
    TARGET_ACCOUNT_ID("targetAccountId");

    private final String rawKey;

    BoardAdminActivityDetailKey(String rawKey) {
        this.rawKey = rawKey;
    }

    static Optional<BoardAdminActivityDetailKey> from(String rawKey) {
        return Arrays.stream(values())
                .filter(key -> key.rawKey.equals(rawKey))
                .findFirst();
    }
}

final class BoardAdminParsedActivity {

    private final AccountActivityLog activity;
    private final BoardAdminActivityAction action;
    private final BoardAdminActivityDetail detail;

    BoardAdminParsedActivity(AccountActivityLog activity,
                             BoardAdminActivityAction action,
                             BoardAdminActivityDetail detail) {
        this.activity = activity;
        this.action = action == null ? BoardAdminActivityAction.UNKNOWN : action;
        this.detail = detail == null ? BoardAdminActivityDetail.empty() : detail;
    }

    AccountActivityLog activity() {
        return activity;
    }

    BoardAdminActivityAction action() {
        return action;
    }

    BoardAdminActivityDetail detail() {
        return detail;
    }
}

enum BoardAdminActivityAction {
    BOARD_CREATE,
    BOARD_UPDATE,
    BOARD_DELETE,
    BOARD_SCRAP_ADD,
    BOARD_SCRAP_REMOVE,
    BOARD_LIKE_TOGGLE,
    BOARD_REPORT_CREATE,
    BOARD_COMMENT_CREATE,
    BOARD_COMMENT_DELETE,
    BOARD_REPORT_STATUS_UPDATE,
    BOOKMARK_ADD,
    BOOKMARK_REMOVE,
    RECIPE_REVIEW_UPSERT,
    ACCOUNT_BLOCK_ADD,
    ACCOUNT_BLOCK_REMOVE,
    NOTIFICATION_SETTING_UPDATE,
    UNKNOWN;

    static BoardAdminActivityAction from(String rawAction) {
        if (rawAction == null || rawAction.trim().isEmpty()) {
            return UNKNOWN;
        }
        try {
            return BoardAdminActivityAction.valueOf(rawAction.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
        }
    }

    String rawValue() {
        return this == UNKNOWN ? "UNKNOWN" : name();
    }
}

final class BoardAdminActivityDetail {
    private final Long boardId;
    private final Long commentId;
    private final Long reportId;
    private final Long recipeId;
    private final Long targetAccountId;

    BoardAdminActivityDetail(Long boardId,
                             Long commentId,
                             Long reportId,
                             Long recipeId,
                             Long targetAccountId) {
        this.boardId = boardId;
        this.commentId = commentId;
        this.reportId = reportId;
        this.recipeId = recipeId;
        this.targetAccountId = targetAccountId;
    }

    static BoardAdminActivityDetail empty() {
        return new BoardAdminActivityDetail(
                null,
                null,
                null,
                null,
                null
        );
    }

    Optional<Long> boardId() {
        return Optional.ofNullable(boardId);
    }

    Optional<Long> commentId() {
        return Optional.ofNullable(commentId);
    }

    Optional<Long> reportId() {
        return Optional.ofNullable(reportId);
    }

    Optional<Long> recipeId() {
        return Optional.ofNullable(recipeId);
    }

    Optional<Long> targetAccountId() {
        return Optional.ofNullable(targetAccountId);
    }
}
