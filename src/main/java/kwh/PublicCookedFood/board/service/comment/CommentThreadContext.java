package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;

import java.util.ArrayList;

public final class CommentThreadContext {

    private final CommentActorBoardContext actorBoard;
    private final ResolvedCommentParent parent;

    private CommentThreadContext(CommentActorBoardContext actorBoard, ResolvedCommentParent parent) {
        this.actorBoard = actorBoard;
        this.parent = parent;
    }

    public static CommentThreadContext forCreate(CommentActorBoardContext actorBoard, ResolvedCommentParent parent) {
        if (actorBoard == null) {
            throw new IllegalArgumentException("actorBoard");
        }
        if (parent == null) {
            throw new IllegalArgumentException("parent");
        }
        return new CommentThreadContext(actorBoard, parent);
    }

    public Comments newComment(String contents) {
        Comments.CommentsBuilder builder = Comments.builder()
                .account(actorBoard.actor())
                .board(actorBoard.board())
                .contents(contents)
                .state(SoftDeleteState.ACTIVE)
                .replies(new ArrayList<>());
        parent.applyTo(builder);
        return builder.build();
    }

    public long initialRootParentId(long savedCommentId) {
        return parent.initialRootParentId(savedCommentId);
    }
}
