package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.board.facade.BoardViewer;

import java.util.Objects;

public final class CommentTargetPathQuery {

    private final long boardId;
    private final long commentId;
    private final BoardViewer viewer;
    private final CommentPageSpec pageSpec;

    public CommentTargetPathQuery(long boardId,
                                  long commentId,
                                  BoardViewer viewer,
                                  CommentPageSpec pageSpec) {
        if (boardId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 게시글입니다.");
        }
        if (commentId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 댓글입니다.");
        }
        this.boardId = boardId;
        this.commentId = commentId;
        this.viewer = viewer == null ? BoardViewer.anonymous() : viewer;
        this.pageSpec = pageSpec == null ? CommentPageSpec.defaultSize() : pageSpec;
    }

    public static CommentTargetPathQuery of(Long boardId, Long commentId, BoardViewer viewer) {
        return new CommentTargetPathQuery(
                Objects.requireNonNull(boardId, "유효하지 않은 게시글입니다."),
                Objects.requireNonNull(commentId, "유효하지 않은 댓글입니다."),
                viewer,
                CommentPageSpec.defaultSize()
        );
    }

    public static CommentTargetPathQuery of(Long boardId,
                                            Long commentId,
                                            BoardViewer viewer,
                                            int pageSize) {
        return new CommentTargetPathQuery(
                Objects.requireNonNull(boardId, "유효하지 않은 게시글입니다."),
                Objects.requireNonNull(commentId, "유효하지 않은 댓글입니다."),
                viewer,
                CommentPageSpec.forTargetPath(pageSize)
        );
    }

    public long boardId() {
        return boardId;
    }

    public long commentId() {
        return commentId;
    }

    public BoardViewer viewer() {
        return viewer;
    }

    public CommentPageSpec pageSpec() {
        return pageSpec;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CommentTargetPathQuery)) {
            return false;
        }
        CommentTargetPathQuery that = (CommentTargetPathQuery) other;
        return boardId == that.boardId
                && commentId == that.commentId
                && Objects.equals(viewer, that.viewer)
                && Objects.equals(pageSpec, that.pageSpec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(boardId, commentId, viewer, pageSpec);
    }
}
