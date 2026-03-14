package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.board.service.support.BoardVisibilityCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CommentReplyLoader {

    private final CommentsRepository commentsRepository;

    public Map<Long, List<Comments>> loadRepliesByRootParentIds(long boardId,
                                                                List<Comments> parentComments,
                                                                BoardVisibilityCriteria visibility) {
        Objects.requireNonNull(parentComments, "parentComments");
        Objects.requireNonNull(visibility, "visibility");
        Set<Long> rootParentIds = extractRootParentIds(parentComments);
        if (rootParentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Comments> replies = commentsRepository.findRepliesWithAccountAndParentByBoardIdAndRootParentIdInOrderByCommentPathAsc(
                boardId,
                rootParentIds,
                visibility.excludeRestricted(),
                visibility.restrictedAccountIdsOrSentinel()
        );
        if (replies.isEmpty()) {
            return Collections.emptyMap();
        }
        return replies.stream()
                .collect(Collectors.groupingBy(reply -> reply.getParent().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private Set<Long> extractRootParentIds(List<Comments> parentComments) {
        if (parentComments.isEmpty()) {
            return Collections.emptySet();
        }
        return parentComments.stream()
                .map(this::requirePersistedRootParentId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Long requirePersistedRootParentId(Comments comment) {
        Objects.requireNonNull(comment, "comment");
        return Objects.requireNonNull(comment.getEffectiveRootParentId(), "comment.effectiveRootParentId");
    }
}
