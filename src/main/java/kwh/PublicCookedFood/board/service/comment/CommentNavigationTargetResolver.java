package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CommentNavigationTargetResolver {

    private final CommentsRepository commentsRepository;
    private final CommentVisibilityPolicy commentVisibilityPolicy;

    public CommentNavigationTargets resolve(CommentTargetPathsQuery query,
                                            Set<Long> blockedAccountIds) {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(blockedAccountIds, "blockedAccountIds");

        Map<Long, Comments> targetCommentsById = commentsRepository.findWithBoardAndParentByIdIn(query.commentIds()).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Comments::getId, comment -> comment, (left, right) -> left, LinkedHashMap::new));

        Map<Long, Long> rootParentIdByCommentId = new LinkedHashMap<>();
        Set<Long> targetRootParentIds = new LinkedHashSet<>();
        for (Long commentId : query.commentIds()) {
            Comments targetComment = targetCommentsById.get(commentId);
            if (!isNavigableTargetComment(targetComment, query.boardId(), blockedAccountIds)) {
                continue;
            }
            long rootParentId = requirePersistedRootParentId(targetComment);
            rootParentIdByCommentId.put(commentId, rootParentId);
            targetRootParentIds.add(rootParentId);
        }

        return new CommentNavigationTargets(targetCommentsById, rootParentIdByCommentId, targetRootParentIds);
    }

    private boolean isNavigableTargetComment(Comments targetComment,
                                             long boardId,
                                             Set<Long> blockedAccountIds) {
        return targetComment != null
                && targetComment.getBoard() != null
                && targetComment.getBoard().getId() != null
                && targetComment.getBoard().getId() == boardId
                && !commentVisibilityPolicy.isBlockedAuthor(targetComment, blockedAccountIds);
    }

    private long requirePersistedRootParentId(Comments comment) {
        return Objects.requireNonNull(comment.getEffectiveRootParentId(), "comment.effectiveRootParentId");
    }
}
