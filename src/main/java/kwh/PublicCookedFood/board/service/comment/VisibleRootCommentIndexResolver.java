package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.service.support.BoardVisibilityCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class VisibleRootCommentIndexResolver {

    private final VisibleRootCommentScanner visibleRootCommentScanner;

    public VisibleRootCommentIndex resolve(long boardId,
                                           Set<Long> targetRootParentIds,
                                           BoardVisibilityCriteria visibility) {
        Objects.requireNonNull(targetRootParentIds, "targetRootParentIds");
        Objects.requireNonNull(visibility, "visibility");
        if (targetRootParentIds.isEmpty()) {
            return new VisibleRootCommentIndex(Collections.<Long, Integer>emptyMap(), Collections.<Long, Comments>emptyMap());
        }
        VisibleRootCommentScanResult scanResult = visibleRootCommentScanner.scan(
                VisibleRootCommentScanQuery.track(boardId, visibility, targetRootParentIds)
        );
        return new VisibleRootCommentIndex(
                scanResult.trackedVisibleParentIndexById(),
                scanResult.trackedVisibleRootCommentsById()
        );
    }
}
