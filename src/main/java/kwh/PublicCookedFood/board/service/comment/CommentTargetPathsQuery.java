package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.board.facade.BoardViewer;

import java.util.Collections;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class CommentTargetPathsQuery {

    private final long boardId;
    private final Set<Long> commentIds;
    private final BoardViewer viewer;
    private final CommentPageSpec pageSpec;

    public CommentTargetPathsQuery(long boardId,
                                   Set<Long> commentIds,
                                   BoardViewer viewer,
                                   CommentPageSpec pageSpec) {
        if (boardId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 게시글입니다.");
        }
        this.boardId = boardId;
        this.commentIds = normalizeCommentIds(commentIds);
        this.viewer = viewer == null ? BoardViewer.anonymous() : viewer;
        this.pageSpec = pageSpec == null ? CommentPageSpec.defaultSize() : pageSpec;
    }

    public static CommentTargetPathsQuery of(Long boardId,
                                             Collection<Long> commentIds,
                                             BoardViewer viewer) {
        return new CommentTargetPathsQuery(
                Objects.requireNonNull(boardId, "유효하지 않은 게시글입니다."),
                normalizeCommentIds(commentIds),
                viewer,
                CommentPageSpec.defaultSize()
        );
    }

    public static CommentTargetPathsQuery of(Long boardId,
                                             Collection<Long> commentIds,
                                             BoardViewer viewer,
                                             int pageSize) {
        return new CommentTargetPathsQuery(
                Objects.requireNonNull(boardId, "유효하지 않은 게시글입니다."),
                normalizeCommentIds(commentIds),
                viewer,
                CommentPageSpec.forTargetPath(pageSize)
        );
    }

    private static Set<Long> normalizeCommentIds(Collection<Long> rawCommentIds) {
        if (Objects.requireNonNull(rawCommentIds, "댓글 ID 목록이 비어 있습니다.").isEmpty()) {
            return Collections.emptySet();
        }
        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        for (Long commentId : rawCommentIds) {
            long resolvedCommentId = Objects.requireNonNull(commentId, "유효하지 않은 댓글이 포함되어 있습니다.");
            if (resolvedCommentId <= 0) {
                throw new IllegalArgumentException("유효하지 않은 댓글이 포함되어 있습니다.");
            }
            normalized.add(resolvedCommentId);
        }
        return Collections.unmodifiableSet(normalized);
    }

    public long boardId() {
        return boardId;
    }

    public Set<Long> commentIds() {
        return commentIds;
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
        if (!(other instanceof CommentTargetPathsQuery)) {
            return false;
        }
        CommentTargetPathsQuery that = (CommentTargetPathsQuery) other;
        return boardId == that.boardId
                && Objects.equals(commentIds, that.commentIds)
                && Objects.equals(viewer, that.viewer)
                && Objects.equals(pageSpec, that.pageSpec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(boardId, commentIds, viewer, pageSpec);
    }
}
