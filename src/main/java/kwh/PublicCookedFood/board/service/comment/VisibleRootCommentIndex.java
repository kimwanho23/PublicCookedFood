package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.board.domain.Comments;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class VisibleRootCommentIndex {

    private final Map<Long, Integer> visibleParentIndexById;
    private final Map<Long, Comments> visibleRootCommentsById;

    public VisibleRootCommentIndex(Map<Long, Integer> visibleParentIndexById,
                                   Map<Long, Comments> visibleRootCommentsById) {
        this.visibleParentIndexById = visibleParentIndexById == null
                ? Collections.<Long, Integer>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<Long, Integer>(visibleParentIndexById));
        this.visibleRootCommentsById = visibleRootCommentsById == null
                ? Collections.<Long, Comments>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<Long, Comments>(visibleRootCommentsById));
    }

    public Map<Long, Integer> visibleParentIndexById() {
        return visibleParentIndexById;
    }

    public Map<Long, Comments> visibleRootCommentsById() {
        return visibleRootCommentsById;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof VisibleRootCommentIndex)) {
            return false;
        }
        VisibleRootCommentIndex that = (VisibleRootCommentIndex) other;
        return Objects.equals(visibleParentIndexById, that.visibleParentIndexById)
                && Objects.equals(visibleRootCommentsById, that.visibleRootCommentsById);
    }

    @Override
    public int hashCode() {
        return Objects.hash(visibleParentIndexById, visibleRootCommentsById);
    }
}
