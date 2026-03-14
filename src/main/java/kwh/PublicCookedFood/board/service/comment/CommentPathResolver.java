package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.board.domain.Comments;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CommentPathResolver {

    private static final int COMMENT_PATH_SEGMENT_WIDTH = 19;
    private static final String COMMENT_PATH_SEPARATOR = "/";

    public String buildPath(long commentId, ResolvedCommentParent parent) {
        Objects.requireNonNull(parent, "parent");
        String currentSegment = formatPathSegment(commentId);
        String parentPath = parent.pathPrefix(this);
        if (parentPath.trim().isEmpty()) {
            return currentSegment;
        }
        return parentPath + COMMENT_PATH_SEPARATOR + currentSegment;
    }

    public String resolvePath(Comments comment) {
        Comments currentComment = Objects.requireNonNull(comment, "comment");
        if (currentComment.getCommentPath() != null && !currentComment.getCommentPath().trim().isEmpty()) {
            return currentComment.getCommentPath();
        }
        long commentId = Objects.requireNonNull(currentComment.getId(), "comment.id");
        if (currentComment.getParent() == null) {
            return formatPathSegment(commentId);
        }
        String parentPath = resolvePath(currentComment.getParent());
        String currentSegment = formatPathSegment(commentId);
        return parentPath + COMMENT_PATH_SEPARATOR + currentSegment;
    }

    private String formatPathSegment(long commentId) {
        return String.format("%0" + COMMENT_PATH_SEGMENT_WIDTH + "d", commentId);
    }
}
