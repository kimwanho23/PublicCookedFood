package kwh.PublicCookedFood.board.service.comment;

import java.util.Objects;

public final class CommentDeleteCommand {

    private final long boardId;
    private final long commentId;
    private final long actorAccountId;

    public CommentDeleteCommand(long boardId, long commentId, long actorAccountId) {
        if (boardId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 게시글입니다.");
        }
        if (commentId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 댓글입니다.");
        }
        if (actorAccountId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자입니다.");
        }
        this.boardId = boardId;
        this.commentId = commentId;
        this.actorAccountId = actorAccountId;
    }

    public static CommentDeleteCommand of(Long actorAccountId, Long boardId, Long commentId) {
        return new CommentDeleteCommand(
                Objects.requireNonNull(boardId, "유효하지 않은 게시글입니다."),
                Objects.requireNonNull(commentId, "유효하지 않은 댓글입니다."),
                Objects.requireNonNull(actorAccountId, "유효하지 않은 사용자입니다.")
        );
    }

    public long boardId() {
        return boardId;
    }

    public long commentId() {
        return commentId;
    }

    public long actorAccountId() {
        return actorAccountId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CommentDeleteCommand)) {
            return false;
        }
        CommentDeleteCommand that = (CommentDeleteCommand) other;
        return boardId == that.boardId
                && commentId == that.commentId
                && actorAccountId == that.actorAccountId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(boardId, commentId, actorAccountId);
    }
}
