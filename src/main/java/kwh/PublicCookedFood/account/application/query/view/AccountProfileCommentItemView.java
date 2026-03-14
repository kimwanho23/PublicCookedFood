package kwh.PublicCookedFood.account.application.query.view;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;

import java.time.LocalDateTime;
import java.util.Objects;

public final class AccountProfileCommentItemView {

    private final Long commentId;
    private final Long boardId;
    private final String boardTitle;
    private final String contents;
    private final LocalDateTime regTime;
    private final String targetPath;

    public AccountProfileCommentItemView(Long commentId,
                                         Long boardId,
                                         String boardTitle,
                                         String contents,
                                         LocalDateTime regTime,
                                         String targetPath) {
        if (commentId == null || commentId.longValue() <= 0L) {
            throw new IllegalArgumentException("commentId must be positive");
        }
        if (boardId == null || boardId.longValue() <= 0L) {
            throw new IllegalArgumentException("boardId must be positive");
        }
        this.commentId = commentId;
        this.boardId = boardId;
        this.boardTitle = boardTitle;
        this.contents = contents;
        this.regTime = regTime;
        this.targetPath = normalizeTargetPath(targetPath);
    }

    public static AccountProfileCommentItemView from(Comments comment, String targetPath) {
        Objects.requireNonNull(comment, "comment");
        Board board = Objects.requireNonNull(comment.getBoard(), "comment.board");
        return new AccountProfileCommentItemView(
                Objects.requireNonNull(comment.getId(), "comment.id"),
                Objects.requireNonNull(board.getId(), "board.id"),
                board.getTitle(),
                comment.getContents(),
                comment.getRegTime(),
                targetPath
        );
    }

    public Long commentId() {
        return commentId;
    }

    public Long boardId() {
        return boardId;
    }

    public String boardTitle() {
        return boardTitle;
    }

    public String contents() {
        return contents;
    }

    public LocalDateTime regTime() {
        return regTime;
    }

    public String targetPath() {
        return targetPath;
    }

    private static String normalizeTargetPath(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("targetPath must not be blank");
        }
        return value.trim();
    }
}
