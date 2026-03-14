package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class CommentVisibilityPolicy {

    public boolean shouldDisplay(Comments comment, Map<Long, List<Comments>> repliesByParentId) {
        Objects.requireNonNull(comment, "comment");
        Objects.requireNonNull(repliesByParentId, "repliesByParentId");
        if (comment.getState() == SoftDeleteState.ACTIVE) {
            return true;
        }
        if (comment.getState() != SoftDeleteState.DELETED) {
            return false;
        }
        return hasVisibleDescendants(requireCommentId(comment), repliesByParentId);
    }

    public boolean isReachable(Comments comment, Map<Long, List<Comments>> repliesByParentId) {
        Objects.requireNonNull(comment, "comment");
        Objects.requireNonNull(repliesByParentId, "repliesByParentId");
        Comments parent = comment.getParent();
        if (parent == null) {
            return true;
        }
        if (!isReachable(parent, repliesByParentId)) {
            return false;
        }
        List<Comments> siblings = repliesByParentId.getOrDefault(requireCommentId(parent), Collections.<Comments>emptyList());
        long commentId = requireCommentId(comment);
        return siblings.stream()
                .map(this::requireCommentId)
                .anyMatch(siblingId -> siblingId == commentId);
    }

    public boolean hasVisibleDescendants(long commentId, Map<Long, List<Comments>> repliesByParentId) {
        Objects.requireNonNull(repliesByParentId, "repliesByParentId");
        List<Comments> children = repliesByParentId.getOrDefault(commentId, Collections.<Comments>emptyList());
        for (Comments child : children) {
            if (child.getState() == SoftDeleteState.ACTIVE) {
                return true;
            }
            if (child.getState() == SoftDeleteState.DELETED
                    && hasVisibleDescendants(requireCommentId(child), repliesByParentId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isBlockedAuthor(Comments comment, Set<Long> blockedAccountIds) {
        Objects.requireNonNull(comment, "comment");
        Objects.requireNonNull(blockedAccountIds, "blockedAccountIds");
        if (blockedAccountIds.isEmpty()) {
            return false;
        }
        return blockedAccountIds.contains(requireAuthorAccountId(comment));
    }

    private long requireCommentId(Comments comment) {
        return Objects.requireNonNull(comment.getId(), "comment.id");
    }

    private long requireAuthorAccountId(Comments comment) {
        return Objects.requireNonNull(
                Objects.requireNonNull(comment.getAccount(), "comment.account").getId(),
                "comment.account.id"
        );
    }
}
