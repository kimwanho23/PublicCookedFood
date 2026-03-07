package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CommentNavigationService {

    public static final int DEFAULT_COMMENT_PAGE_SIZE = 50;
    public static final int MAX_COMMENT_PAGE_SIZE = 100;
    private static final int COMMENT_SCAN_BATCH_SIZE = 100;

    private final CommentsRepository commentsRepository;
    private final AccountBlockService accountBlockService;

    @Transactional(readOnly = true)
    public String buildCommentTargetPath(Long boardId, Long commentId, Long viewerAccountId) {
        return buildCommentTargetPath(boardId, commentId, viewerAccountId, DEFAULT_COMMENT_PAGE_SIZE);
    }

    @Transactional(readOnly = true)
    public String buildCommentTargetPath(Long boardId,
                                         Long commentId,
                                         Long viewerAccountId,
                                         int pageSize) {
        return buildCommentTargetPaths(boardId, List.of(commentId), viewerAccountId, pageSize)
                .getOrDefault(commentId, defaultBoardCommentsPath(boardId));
    }

    @Transactional(readOnly = true)
    public Map<Long, String> buildCommentTargetPaths(Long boardId,
                                                     Collection<Long> commentIds,
                                                     Long viewerAccountId) {
        return buildCommentTargetPaths(boardId, commentIds, viewerAccountId, DEFAULT_COMMENT_PAGE_SIZE);
    }

    @Transactional(readOnly = true)
    public Map<Long, String> buildCommentTargetPaths(Long boardId,
                                                     Collection<Long> commentIds,
                                                     Long viewerAccountId,
                                                     int pageSize) {
        if (boardId == null) {
            return Map.of();
        }
        if (commentIds == null || commentIds.isEmpty()) {
            return Map.of();
        }
        String basePath = "/boards/" + boardId;
        int normalizedPageSize = normalizePageSize(pageSize);
        Set<Long> requestedCommentIds = commentIds.stream()
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (requestedCommentIds.isEmpty()) {
            return Map.of();
        }

        Set<Long> blockedAccountIds = accountBlockService.getViewRestrictedAccountIds(viewerAccountId);
        BlockedAccountFilter blockedAccountFilter = resolveBlockedAccountFilter(blockedAccountIds);
        Map<Long, Comments> targetCommentsById = commentsRepository.findWithBoardAndParentByIdIn(requestedCommentIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Comments::getId, comment -> comment, (left, right) -> left, LinkedHashMap::new));

        Map<Long, Long> rootParentIdByCommentId = new LinkedHashMap<>();
        Set<Long> targetRootParentIds = new java.util.LinkedHashSet<>();
        for (Long commentId : requestedCommentIds) {
            Comments targetComment = targetCommentsById.get(commentId);
            if (targetComment == null
                    || targetComment.getBoard() == null
                    || !boardId.equals(targetComment.getBoard().getId())
                    || isBlockedAuthor(targetComment, blockedAccountIds)) {
                continue;
            }
            Long rootParentId = targetComment.getEffectiveRootParentId();
            if (rootParentId == null) {
                continue;
            }
            rootParentIdByCommentId.put(commentId, rootParentId);
            targetRootParentIds.add(rootParentId);
        }

        Map<Long, Integer> visibleParentIndexById = new LinkedHashMap<>();
        Map<Long, Comments> visibleRootCommentsById = new LinkedHashMap<>();
        int visibleParentIndex = 0;
        int batchPage = 0;

        while (!targetRootParentIds.isEmpty()) {
            Page<Comments> parentBatch = commentsRepository.findParentCommentsPageWithAccountByBoardIdOrderByRegTimeAsc(
                    boardId,
                    SoftDeleteState.ACTIVE,
                    SoftDeleteState.DELETED,
                    !blockedAccountIds.isEmpty(),
                    blockedAccountIds.isEmpty() ? Set.of(-1L) : blockedAccountIds,
                    PageRequest.of(batchPage, COMMENT_SCAN_BATCH_SIZE)
            );
            if (parentBatch.isEmpty()) {
                break;
            }

            List<Comments> batchParents = parentBatch.getContent();
            Map<Long, List<Comments>> batchRepliesByParentId = loadRepliesByRootParentIds(boardId, batchParents, blockedAccountFilter);
            for (Comments parentComment : batchParents) {
                if (!shouldDisplayComment(parentComment, batchRepliesByParentId)) {
                    continue;
                }
                Long parentId = parentComment.getId();
                if (parentId != null && targetRootParentIds.contains(parentId)) {
                    visibleParentIndexById.put(parentId, visibleParentIndex);
                    visibleRootCommentsById.put(parentId, parentComment);
                    targetRootParentIds.remove(parentId);
                }
                visibleParentIndex++;
            }

            if (parentBatch.isLast()) {
                break;
            }
            batchPage++;
        }

        Map<Long, List<Comments>> requestedRootRepliesByParentId = loadRepliesByRootParentIds(
                boardId,
                new ArrayList<>(visibleRootCommentsById.values()),
                blockedAccountFilter
        );

        Map<Long, String> targetPaths = new LinkedHashMap<>();
        for (Long commentId : requestedCommentIds) {
            Comments targetComment = targetCommentsById.get(commentId);
            if (targetComment == null
                    || targetComment.getBoard() == null
                    || !boardId.equals(targetComment.getBoard().getId())
                    || isBlockedAuthor(targetComment, blockedAccountIds)) {
                targetPaths.put(commentId, basePath + "#board-comments");
                continue;
            }

            boolean targetVisible = isReachableComment(targetComment, requestedRootRepliesByParentId)
                    && shouldDisplayComment(targetComment, requestedRootRepliesByParentId);
            Long rootParentId = rootParentIdByCommentId.get(commentId);
            Integer parentIndex = rootParentId == null ? null : visibleParentIndexById.get(rootParentId);
            if (parentIndex == null) {
                targetPaths.put(commentId, basePath + "#board-comments");
                continue;
            }

            int pageNumber = parentIndex / normalizedPageSize;
            String anchor = targetVisible ? "#comment-" + commentId : "#board-comments";
            targetPaths.put(commentId, basePath + "?commentPage=" + pageNumber + "&commentSize=" + normalizedPageSize + anchor);
        }
        return Map.copyOf(targetPaths);
    }

    private Map<Long, List<Comments>> loadRepliesByRootParentIds(Long boardId,
                                                                 List<Comments> parentComments,
                                                                 BlockedAccountFilter blockedAccountFilter) {
        Set<Long> rootParentIds = extractRootParentIds(parentComments);
        if (boardId == null || rootParentIds.isEmpty()) {
            return Map.of();
        }
        List<Comments> replies = commentsRepository.findRepliesWithAccountAndParentByBoardIdAndRootParentIdInOrderByCommentPathAsc(
                boardId,
                rootParentIds,
                blockedAccountFilter.excludeBlocked(),
                blockedAccountFilter.blockedAccountIds()
        );
        if (replies.isEmpty()) {
            return Map.of();
        }
        return replies.stream()
                .collect(Collectors.groupingBy(reply -> reply.getParent().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private int normalizePageSize(int pageSize) {
        return Math.min(Math.max(pageSize, 1), MAX_COMMENT_PAGE_SIZE);
    }

    private String defaultBoardCommentsPath(Long boardId) {
        if (boardId == null) {
            return "/boards";
        }
        return "/boards/" + boardId + "#board-comments";
    }

    private Set<Long> extractRootParentIds(List<Comments> parentComments) {
        if (parentComments == null || parentComments.isEmpty()) {
            return Set.of();
        }
        return parentComments.stream()
                .map(Comments::getEffectiveRootParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private boolean shouldDisplayComment(Comments comment, Map<Long, List<Comments>> repliesByParentId) {
        if (comment == null) {
            return false;
        }
        if (comment.getState() == SoftDeleteState.ACTIVE) {
            return true;
        }
        if (comment.getState() != SoftDeleteState.DELETED || comment.getId() == null) {
            return false;
        }
        return hasVisibleDescendants(comment.getId(), repliesByParentId);
    }

    private boolean isReachableComment(Comments comment, Map<Long, List<Comments>> repliesByParentId) {
        if (comment == null) {
            return false;
        }
        Comments parent = comment.getParent();
        if (parent == null) {
            return true;
        }
        if (!isReachableComment(parent, repliesByParentId)) {
            return false;
        }
        List<Comments> siblings = repliesByParentId.getOrDefault(parent.getId(), List.of());
        Long commentId = comment.getId();
        if (commentId == null) {
            return false;
        }
        return siblings.stream()
                .map(Comments::getId)
                .anyMatch(commentId::equals);
    }

    private boolean hasVisibleDescendants(Long commentId, Map<Long, List<Comments>> repliesByParentId) {
        if (commentId == null) {
            return false;
        }

        List<Comments> children = repliesByParentId.getOrDefault(commentId, List.of());
        for (Comments child : children) {
            if (child.getState() == SoftDeleteState.ACTIVE) {
                return true;
            }
            if (child.getState() == SoftDeleteState.DELETED && hasVisibleDescendants(child.getId(), repliesByParentId)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlockedAuthor(Comments comment, Set<Long> blockedAccountIds) {
        if (comment == null
                || comment.getAccount() == null
                || comment.getAccount().getId() == null
                || blockedAccountIds == null
                || blockedAccountIds.isEmpty()) {
            return false;
        }
        return blockedAccountIds.contains(comment.getAccount().getId());
    }

    private BlockedAccountFilter resolveBlockedAccountFilter(Set<Long> blockedAccountIds) {
        if (blockedAccountIds == null || blockedAccountIds.isEmpty()) {
            return new BlockedAccountFilter(false, Set.of(-1L));
        }
        Set<Long> normalized = blockedAccountIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (normalized.isEmpty()) {
            return new BlockedAccountFilter(false, Set.of(-1L));
        }
        return new BlockedAccountFilter(true, normalized);
    }

    private record BlockedAccountFilter(boolean excludeBlocked, Set<Long> blockedAccountIds) {
    }
}
