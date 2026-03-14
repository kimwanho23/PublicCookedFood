package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.board.domain.Comments;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VisibleRootCommentScanResult {

    private final List<Comments> selectedVisibleRootComments;
    private final long totalVisibleRootCount;
    private final Map<Long, Integer> trackedVisibleParentIndexById;
    private final Map<Long, Comments> trackedVisibleRootCommentsById;

    public VisibleRootCommentScanResult(List<Comments> selectedVisibleRootComments,
                                        long totalVisibleRootCount,
                                        Map<Long, Integer> trackedVisibleParentIndexById,
                                        Map<Long, Comments> trackedVisibleRootCommentsById) {
        if (totalVisibleRootCount < 0L) {
            throw new IllegalArgumentException("totalVisibleRootCount must not be negative");
        }
        this.selectedVisibleRootComments = selectedVisibleRootComments == null
                ? Collections.<Comments>emptyList()
                : Collections.unmodifiableList(selectedVisibleRootComments);
        this.totalVisibleRootCount = totalVisibleRootCount;
        this.trackedVisibleParentIndexById = trackedVisibleParentIndexById == null
                ? Collections.<Long, Integer>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<Long, Integer>(trackedVisibleParentIndexById));
        this.trackedVisibleRootCommentsById = trackedVisibleRootCommentsById == null
                ? Collections.<Long, Comments>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<Long, Comments>(trackedVisibleRootCommentsById));
    }

    public List<Comments> selectedVisibleRootComments() {
        return selectedVisibleRootComments;
    }

    public long totalVisibleRootCount() {
        return totalVisibleRootCount;
    }

    public Map<Long, Integer> trackedVisibleParentIndexById() {
        return trackedVisibleParentIndexById;
    }

    public Map<Long, Comments> trackedVisibleRootCommentsById() {
        return trackedVisibleRootCommentsById;
    }
}
