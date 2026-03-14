package kwh.PublicCookedFood.board.service.comment;

import java.util.Objects;

public final class CommentCreateCommand {

    private final long accountId;
    private final long boardId;
    private final String contents;
    private final CommentParentTarget target;

    public CommentCreateCommand(long accountId,
                                long boardId,
                                String contents,
                                CommentParentTarget target) {
        if (accountId <= 0) {
            throw new IllegalArgumentException("Invalid account ID");
        }
        if (boardId <= 0) {
            throw new IllegalArgumentException("Invalid board ID");
        }
        if (contents == null || contents.trim().isEmpty()) {
            throw new IllegalArgumentException("댓글 내용은 필수입니다.");
        }
        this.accountId = accountId;
        this.boardId = boardId;
        this.contents = contents;
        this.target = target == null ? CommentParentTarget.root() : target;
    }

    public static CommentCreateCommand of(Long accountId,
                                          Long boardId,
                                          String contents,
                                          Long parentId) {
        return new CommentCreateCommand(
                Objects.requireNonNull(accountId, "Invalid account ID"),
                Objects.requireNonNull(boardId, "Invalid board ID"),
                contents,
                resolveTarget(parentId)
        );
    }

    public long accountId() {
        return accountId;
    }

    public long boardId() {
        return boardId;
    }

    public String contents() {
        return contents;
    }

    public CommentParentTarget target() {
        return target;
    }

    public boolean hasParent() {
        return target.isReply();
    }

    public long requiredParentId() {
        return target.requireParentId();
    }

    private static CommentParentTarget resolveTarget(Long parentId) {
        if (parentId == null) {
            return CommentParentTarget.root();
        }
        return CommentParentTarget.reply(parentId);
    }
}
