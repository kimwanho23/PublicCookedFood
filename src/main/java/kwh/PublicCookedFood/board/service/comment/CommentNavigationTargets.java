package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.board.domain.Comments;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class CommentNavigationTargets {

    private final Map<Long, Comments> targetCommentsById;
    private final Map<Long, Long> rootParentIdByCommentId;
    private final Set<Long> targetRootParentIds;

    public CommentNavigationTargets(Map<Long, Comments> targetCommentsById,
                                    Map<Long, Long> rootParentIdByCommentId,
                                    Set<Long> targetRootParentIds) {
        this.targetCommentsById = targetCommentsById == null
                ? Collections.<Long, Comments>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<Long, Comments>(targetCommentsById));
        this.rootParentIdByCommentId = rootParentIdByCommentId == null
                ? Collections.<Long, Long>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<Long, Long>(rootParentIdByCommentId));
        this.targetRootParentIds = targetRootParentIds == null
                ? Collections.<Long>emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<Long>(targetRootParentIds));
    }

    public Map<Long, Comments> targetCommentsById() {
        return targetCommentsById;
    }

    public Map<Long, Long> rootParentIdByCommentId() {
        return rootParentIdByCommentId;
    }

    public Set<Long> targetRootParentIds() {
        return targetRootParentIds;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CommentNavigationTargets)) {
            return false;
        }
        CommentNavigationTargets that = (CommentNavigationTargets) other;
        return Objects.equals(targetCommentsById, that.targetCommentsById)
                && Objects.equals(rootParentIdByCommentId, that.rootParentIdByCommentId)
                && Objects.equals(targetRootParentIds, that.targetRootParentIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetCommentsById, rootParentIdByCommentId, targetRootParentIds);
    }
}
