package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.board.service.support.BoardVisibilityCriteria;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class VisibleRootCommentScanQuery {

    private final long boardId;
    private final BoardVisibilityCriteria visibility;
    private final VisibleRootCommentSelection selection;
    private final Set<Long> trackedRootParentIds;
    private final boolean countTotalVisible;

    public VisibleRootCommentScanQuery(long boardId,
                                       BoardVisibilityCriteria visibility,
                                       VisibleRootCommentSelection selection,
                                       Set<Long> trackedRootParentIds,
                                       boolean countTotalVisible) {
        if (boardId <= 0L) {
            throw new IllegalArgumentException("boardId must be positive");
        }
        this.boardId = boardId;
        this.visibility = java.util.Objects.requireNonNull(visibility, "visibility");
        this.selection = java.util.Objects.requireNonNull(selection, "selection");
        this.trackedRootParentIds = normalizeTrackedRootParentIds(trackedRootParentIds);
        this.countTotalVisible = countTotalVisible;
    }

    public static VisibleRootCommentScanQuery all(long boardId,
                                                  BoardVisibilityCriteria visibility) {
        return new VisibleRootCommentScanQuery(
                boardId,
                visibility,
                VisibleRootCommentSelection.all(),
                Collections.<Long>emptySet(),
                true
        );
    }

    public static VisibleRootCommentScanQuery page(long boardId,
                                                   BoardVisibilityCriteria visibility,
                                                   long startInclusive,
                                                   long endExclusive) {
        return new VisibleRootCommentScanQuery(
                boardId,
                visibility,
                VisibleRootCommentSelection.range(startInclusive, endExclusive),
                Collections.<Long>emptySet(),
                true
        );
    }

    public static VisibleRootCommentScanQuery track(long boardId,
                                                    BoardVisibilityCriteria visibility,
                                                    Set<Long> trackedRootParentIds) {
        return new VisibleRootCommentScanQuery(
                boardId,
                visibility,
                VisibleRootCommentSelection.none(),
                trackedRootParentIds,
                false
        );
    }

    public long boardId() {
        return boardId;
    }

    public BoardVisibilityCriteria visibility() {
        return visibility;
    }

    public VisibleRootCommentSelection selection() {
        return selection;
    }

    public Set<Long> trackedRootParentIds() {
        return trackedRootParentIds;
    }

    public boolean countTotalVisible() {
        return countTotalVisible;
    }

    private static Set<Long> normalizeTrackedRootParentIds(Set<Long> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptySet();
        }
        LinkedHashSet<Long> normalized = new LinkedHashSet<Long>();
        for (Long rootParentId : source) {
            long resolvedRootParentId = java.util.Objects.requireNonNull(rootParentId, "trackedRootParentIds contains null");
            if (resolvedRootParentId <= 0L) {
                throw new IllegalArgumentException("trackedRootParentIds must be positive");
            }
            normalized.add(resolvedRootParentId);
        }
        return Collections.unmodifiableSet(new LinkedHashSet<Long>(normalized));
    }
}
