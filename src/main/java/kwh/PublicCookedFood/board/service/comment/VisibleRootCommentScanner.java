package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class VisibleRootCommentScanner {

    private static final int COMMENT_SCAN_BATCH_SIZE = 100;

    private final CommentsRepository commentsRepository;
    private final CommentReplyLoader commentReplyLoader;
    private final CommentVisibilityPolicy commentVisibilityPolicy;

    public VisibleRootCommentScanResult scan(VisibleRootCommentScanQuery query) {
        Objects.requireNonNull(query, "query");

        List<Comments> selectedVisibleRootComments = new ArrayList<>();
        Map<Long, Integer> trackedVisibleParentIndexById = new LinkedHashMap<>();
        Map<Long, Comments> trackedVisibleRootCommentsById = new LinkedHashMap<>();
        Set<Long> unresolvedTrackedRootParentIds = new LinkedHashSet<>(query.trackedRootParentIds());

        long visibleRootCount = 0L;
        int batchPage = 0;

        while (true) {
            Page<Comments> parentBatch = commentsRepository.findParentCommentsPageWithAccountByBoardIdOrderByRegTimeAsc(
                    query.boardId(),
                    SoftDeleteState.ACTIVE,
                    SoftDeleteState.DELETED,
                    query.visibility().excludeRestricted(),
                    query.visibility().restrictedAccountIdsOrSentinel(),
                    PageRequest.of(batchPage, COMMENT_SCAN_BATCH_SIZE)
            );
            if (parentBatch.isEmpty()) {
                break;
            }

            List<Comments> batchParents = parentBatch.getContent();
            Map<Long, List<Comments>> batchRepliesByParentId =
                    commentReplyLoader.loadRepliesByRootParentIds(query.boardId(), batchParents, query.visibility());
            for (Comments parentComment : batchParents) {
                if (!commentVisibilityPolicy.shouldDisplay(parentComment, batchRepliesByParentId)) {
                    continue;
                }
                if (query.selection().includes(visibleRootCount)) {
                    selectedVisibleRootComments.add(parentComment);
                }

                Long parentId = parentComment.getId();
                if (parentId != null && unresolvedTrackedRootParentIds.contains(parentId)) {
                    trackedVisibleParentIndexById.put(parentId, Math.toIntExact(visibleRootCount));
                    trackedVisibleRootCommentsById.put(parentId, parentComment);
                    unresolvedTrackedRootParentIds.remove(parentId);
                }
                visibleRootCount++;
            }

            if (parentBatch.isLast()) {
                break;
            }
            if (shouldStopEarly(query, unresolvedTrackedRootParentIds, visibleRootCount)) {
                break;
            }
            batchPage++;
        }

        return new VisibleRootCommentScanResult(
                selectedVisibleRootComments,
                visibleRootCount,
                trackedVisibleParentIndexById,
                trackedVisibleRootCommentsById
        );
    }

    private boolean shouldStopEarly(VisibleRootCommentScanQuery query,
                                    Set<Long> unresolvedTrackedRootParentIds,
                                    long visibleRootCount) {
        return !query.countTotalVisible()
                && unresolvedTrackedRootParentIds.isEmpty()
                && query.selection().isSatisfiedAfter(visibleRootCount);
    }
}
