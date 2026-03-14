package kwh.PublicCookedFood.board.application.query.view;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.Objects;

@Getter
public final class CommentThreadPageView {

    private final Long boardId;
    private final Long currentAccountId;
    private final boolean boardInteractionBlocked;
    private final Page<CommentNodeView> comments;

    public CommentThreadPageView(Long boardId,
                                 Long currentAccountId,
                                 boolean boardInteractionBlocked,
                                 Page<CommentNodeView> comments) {
        this.boardId = Objects.requireNonNull(boardId, "boardId");
        this.currentAccountId = currentAccountId;
        this.boardInteractionBlocked = boardInteractionBlocked;
        this.comments = Objects.requireNonNull(comments, "comments");
    }

    public Long boardId() {
        return boardId;
    }

    public Long currentAccountId() {
        return currentAccountId;
    }

    public boolean boardInteractionBlocked() {
        return boardInteractionBlocked;
    }

    public Page<CommentNodeView> comments() {
        return comments;
    }
}
