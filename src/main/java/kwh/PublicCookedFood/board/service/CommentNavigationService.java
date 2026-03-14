package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.facade.BoardViewer;
import kwh.PublicCookedFood.board.service.comment.CommentNavigationTargetResolver;
import kwh.PublicCookedFood.board.service.comment.CommentNavigationTargets;
import kwh.PublicCookedFood.board.service.comment.CommentTargetPath;
import kwh.PublicCookedFood.board.service.comment.CommentTargetPathQuery;
import kwh.PublicCookedFood.board.service.comment.CommentTargetPathsQuery;
import kwh.PublicCookedFood.board.service.comment.CommentReplyLoader;
import kwh.PublicCookedFood.board.service.comment.CommentVisibilityPolicy;
import kwh.PublicCookedFood.board.service.comment.VisibleRootCommentIndex;
import kwh.PublicCookedFood.board.service.comment.VisibleRootCommentIndexResolver;
import kwh.PublicCookedFood.board.service.support.BoardVisibilityCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CommentNavigationService {

    public static final int DEFAULT_COMMENT_PAGE_SIZE = 50;
    public static final int MAX_COMMENT_PAGE_SIZE = 100;

    private final AccountBlockService accountBlockService;
    private final CommentReplyLoader commentReplyLoader;
    private final CommentVisibilityPolicy commentVisibilityPolicy;
    private final CommentNavigationTargetResolver commentNavigationTargetResolver;
    private final VisibleRootCommentIndexResolver visibleRootCommentIndexResolver;

    @Transactional(readOnly = true)
    public CommentTargetPath buildCommentTargetPath(CommentTargetPathQuery query) {
        return buildCommentTargetPaths(CommentTargetPathsQuery.of(
                query.boardId(),
                Collections.singletonList(query.commentId()),
                query.viewer(),
                query.pageSpec().pageSize()))
                .getOrDefault(query.commentId(), defaultBoardCommentsPath(query.boardId()));
    }

    @Transactional(readOnly = true)
    public Map<Long, CommentTargetPath> buildCommentTargetPaths(CommentTargetPathsQuery query) {
        if (query.commentIds().isEmpty()) {
            return Collections.emptyMap();
        }
        String basePath = "/boards/" + query.boardId();
        int normalizedPageSize = query.pageSpec().pageSize();
        Set<Long> requestedCommentIds = query.commentIds();

        Set<Long> blockedAccountIds = restrictedAccountIds(query.viewer());
        BoardVisibilityCriteria visibility = BoardVisibilityCriteria.of(blockedAccountIds);
        CommentNavigationTargets targets = commentNavigationTargetResolver.resolve(query, blockedAccountIds);
        VisibleRootCommentIndex visibleRootIndex = visibleRootCommentIndexResolver.resolve(
                query.boardId(),
                targets.targetRootParentIds(),
                visibility
        );

        Map<Long, List<Comments>> requestedRootRepliesByParentId = commentReplyLoader.loadRepliesByRootParentIds(
                query.boardId(),
                new ArrayList<>(visibleRootIndex.visibleRootCommentsById().values()),
                visibility
        );

        Map<Long, CommentTargetPath> targetPaths = new LinkedHashMap<>();
        for (Long commentId : requestedCommentIds) {
            Comments targetComment = targets.targetCommentsById().get(commentId);
            if (!isNavigableTargetComment(targetComment, query.boardId(), blockedAccountIds)) {
                targetPaths.put(commentId, CommentTargetPath.of(basePath + "#board-comments"));
                continue;
            }

            boolean targetVisible = commentVisibilityPolicy.isReachable(targetComment, requestedRootRepliesByParentId)
                    && commentVisibilityPolicy.shouldDisplay(targetComment, requestedRootRepliesByParentId);
            if (!targets.rootParentIdByCommentId().containsKey(commentId)) {
                targetPaths.put(commentId, defaultBoardCommentsPath(query.boardId()));
                continue;
            }
            long rootParentId = targets.rootParentIdByCommentId().get(commentId);
            if (!visibleRootIndex.visibleParentIndexById().containsKey(rootParentId)) {
                targetPaths.put(commentId, defaultBoardCommentsPath(query.boardId()));
                continue;
            }
            int parentIndex = visibleRootIndex.visibleParentIndexById().get(rootParentId);

            int pageNumber = parentIndex / normalizedPageSize;
            String anchor = targetVisible ? "#comment-" + commentId : "#board-comments";
            targetPaths.put(
                    commentId,
                    CommentTargetPath.of(basePath + "?commentPage=" + pageNumber + "&commentSize=" + normalizedPageSize + anchor)
            );
        }
        return Collections.unmodifiableMap(new LinkedHashMap<Long, CommentTargetPath>(targetPaths));
    }

    private CommentTargetPath defaultBoardCommentsPath(long boardId) {
        return CommentTargetPath.of("/boards/" + boardId + "#board-comments");
    }

    private Set<Long> restrictedAccountIds(BoardViewer viewer) {
        Objects.requireNonNull(viewer, "viewer");
        return viewer.maybeAccountId()
                .map(accountBlockService::getViewRestrictedAccountIds)
                .orElseGet(Collections::emptySet);
    }

    private boolean isNavigableTargetComment(Comments targetComment, long boardId, Set<Long> blockedAccountIds) {
        return targetComment != null
                && targetComment.getBoard() != null
                && targetComment.getBoard().getId() != null
                && targetComment.getBoard().getId() == boardId
                && !commentVisibilityPolicy.isBlockedAuthor(targetComment, blockedAccountIds);
    }
}
