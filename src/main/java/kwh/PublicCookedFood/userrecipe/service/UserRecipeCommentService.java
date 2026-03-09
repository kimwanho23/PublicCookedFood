package kwh.PublicCookedFood.userrecipe.service;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipe;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipeComment;
import kwh.PublicCookedFood.userrecipe.dto.request.UserRecipeCommentCreateRequest;
import kwh.PublicCookedFood.userrecipe.dto.response.UserRecipeCommentResponse;
import kwh.PublicCookedFood.userrecipe.error.UserRecipeErrorCode;
import kwh.PublicCookedFood.userrecipe.repository.UserRecipeCommentRepository;
import kwh.PublicCookedFood.userrecipe.repository.UserRecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRecipeCommentService {

    private static final int COMMENT_SCAN_BATCH_SIZE = 100;
    private static final int COMMENT_PATH_SEGMENT_WIDTH = 19;
    private static final String COMMENT_PATH_SEPARATOR = "/";

    private final UserRecipeCommentRepository userRecipeCommentRepository;
    private final UserRecipeRepository userRecipeRepository;
    private final AccountRepository accountRepository;
    private final AccountBlockService accountBlockService;

    @Transactional
    public void deleteComment(Long id) {
        userRecipeCommentRepository.updateState(id, SoftDeleteState.DELETED);
    }

    @Transactional
    public UserRecipeCommentResponse createComment(UserRecipeCommentCreateRequest request) {
        if (request == null || request.getAccountId() == null || request.getRecipeId() == null) {
            throw new IllegalArgumentException("댓글 요청 값이 올바르지 않습니다.");
        }
        UserRecipeComment parent = null;
        if (request.getParentId() != null) {
            parent = userRecipeCommentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("부모 댓글 정보를 찾을 수 없습니다."));
            if (parent.getRecipe() == null
                    || parent.getRecipe().getId() == null
                    || !parent.getRecipe().getId().equals(request.getRecipeId())) {
                throw new IllegalArgumentException("부모 댓글 정보가 올바르지 않습니다.");
            }
            if (parent.getState() != SoftDeleteState.ACTIVE) {
                throw new IllegalArgumentException("삭제된 댓글에는 답글을 작성할 수 없습니다.");
            }
        }

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));
        UserRecipe recipe = userRecipeRepository.findByIdWithAccountAndState(request.getRecipeId(), SoftDeleteState.ACTIVE)
                .orElseThrow(() -> new AppException(UserRecipeErrorCode.USER_RECIPE_NOT_FOUND));

        if (recipe.getAccount() != null && accountBlockService.isEitherBlocked(account.getId(), recipe.getAccount().getId())) {
            throw new AppException(UserRecipeErrorCode.USER_RECIPE_COMMENT_BLOCKED);
        }
        if (parent != null && parent.getAccount() != null && accountBlockService.isEitherBlocked(account.getId(), parent.getAccount().getId())) {
            throw new AppException(UserRecipeErrorCode.USER_RECIPE_COMMENT_BLOCKED);
        }

        UserRecipeComment comment = UserRecipeComment.builder()
                .account(account)
                .recipe(recipe)
                .contents(request.getContents())
                .parent(parent)
                .rootParentId(parent == null ? null : parent.getEffectiveRootParentId())
                .depth(parent == null ? 0 : parent.getDepth() + 1)
                .state(request.getState() == null ? SoftDeleteState.ACTIVE : request.getState())
                .replies(new ArrayList<>())
                .build();

        UserRecipeComment savedComment = userRecipeCommentRepository.save(comment);
        String commentPath = buildCommentPath(savedComment.getId(), parent);
        if (parent == null) {
            savedComment.initializeThreadMetadata(savedComment.getId(), 0, commentPath);
        } else {
            savedComment.initializeThreadMetadata(parent.getEffectiveRootParentId(), parent.getDepth() + 1, commentPath);
        }
        return convertToDto(savedComment);
    }

    @Transactional(readOnly = true)
    public Long getCommentsCount(Long recipeId, Long viewerAccountId) {
        Set<Long> blockedAccountIds = accountBlockService.getViewRestrictedAccountIds(viewerAccountId);
        if (blockedAccountIds.isEmpty()) {
            return userRecipeCommentRepository.countByRecipeIdAndState(recipeId, SoftDeleteState.ACTIVE);
        }
        return userRecipeCommentRepository.countByRecipeIdAndStateAndAccountIdNotIn(
                recipeId,
                SoftDeleteState.ACTIVE,
                blockedAccountIds
        );
    }

    @Transactional(readOnly = true)
    public UserRecipeComment getComment(Long id) {
        return userRecipeCommentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("댓글 정보를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Page<UserRecipeCommentResponse> getCommentListWithReplies(Long recipeId, Pageable pageable, Long viewerAccountId) {
        Set<Long> blockedAccountIds = accountBlockService.getViewRestrictedAccountIds(viewerAccountId);
        BlockedAccountFilter blockedAccountFilter = resolveBlockedAccountFilter(blockedAccountIds);
        Pageable effectivePageable = pageable == null || pageable.isUnpaged()
                ? Pageable.unpaged()
                : PageRequest.of(Math.max(pageable.getPageNumber(), 0), Math.max(pageable.getPageSize(), 1), pageable.getSort());

        if (effectivePageable.isUnpaged()) {
            List<UserRecipeComment> parentComments = userRecipeCommentRepository.findParentCommentsWithAccountByRecipeIdOrderByRegTimeAsc(
                    recipeId,
                    SoftDeleteState.ACTIVE,
                    SoftDeleteState.DELETED,
                    blockedAccountFilter.excludeBlocked(),
                    blockedAccountFilter.blockedAccountIds()
            );
            Map<Long, List<UserRecipeComment>> repliesByParentId = loadRepliesByRootParentIds(recipeId, parentComments, blockedAccountFilter);
            List<UserRecipeComment> visibleParentComments = parentComments.stream()
                    .filter(comment -> shouldDisplayComment(comment, repliesByParentId))
                    .toList();
            List<UserRecipeCommentResponse> content = visibleParentComments.stream()
                    .map(comment -> convertToDto(comment, repliesByParentId, recipeId))
                    .toList();
            return new PageImpl<>(content, Pageable.unpaged(), visibleParentComments.size());
        }

        long pageStartIndex = effectivePageable.getOffset();
        long pageEndExclusive = pageStartIndex + effectivePageable.getPageSize();
        List<UserRecipeComment> selectedParentComments = new ArrayList<>();
        int visibleParentCount = 0;
        int batchPage = 0;

        while (true) {
            Page<UserRecipeComment> parentBatch = userRecipeCommentRepository.findParentCommentsPageWithAccountByRecipeIdOrderByRegTimeAsc(
                    recipeId,
                    SoftDeleteState.ACTIVE,
                    SoftDeleteState.DELETED,
                    blockedAccountFilter.excludeBlocked(),
                    blockedAccountFilter.blockedAccountIds(),
                    PageRequest.of(batchPage, COMMENT_SCAN_BATCH_SIZE)
            );
            if (parentBatch.isEmpty()) {
                break;
            }

            List<UserRecipeComment> batchParents = parentBatch.getContent();
            Map<Long, List<UserRecipeComment>> batchRepliesByParentId = loadRepliesByRootParentIds(recipeId, batchParents, blockedAccountFilter);
            for (UserRecipeComment parentComment : batchParents) {
                if (!shouldDisplayComment(parentComment, batchRepliesByParentId)) {
                    continue;
                }
                if (visibleParentCount >= pageStartIndex && visibleParentCount < pageEndExclusive) {
                    selectedParentComments.add(parentComment);
                }
                visibleParentCount++;
            }

            if (parentBatch.isLast()) {
                break;
            }
            batchPage++;
        }

        Map<Long, List<UserRecipeComment>> pageRepliesByParentId = loadRepliesByRootParentIds(recipeId, selectedParentComments, blockedAccountFilter);
        List<UserRecipeCommentResponse> content = selectedParentComments.stream()
                .map(comment -> convertToDto(comment, pageRepliesByParentId, recipeId))
                .toList();
        return new PageImpl<>(content, effectivePageable, visibleParentCount);
    }

    private Map<Long, List<UserRecipeComment>> loadRepliesByRootParentIds(Long recipeId,
                                                                          List<UserRecipeComment> parentComments,
                                                                          BlockedAccountFilter blockedAccountFilter) {
        Set<Long> rootParentIds = extractRootParentIds(parentComments);
        if (recipeId == null || rootParentIds.isEmpty()) {
            return Map.of();
        }
        List<UserRecipeComment> replies = userRecipeCommentRepository.findRepliesWithAccountAndParentByRecipeIdAndRootParentIdInOrderByCommentPathAsc(
                recipeId,
                rootParentIds,
                blockedAccountFilter.excludeBlocked(),
                blockedAccountFilter.blockedAccountIds()
        );
        if (replies.isEmpty()) {
            return Map.of();
        }
        return replies.stream()
                .collect(Collectors.groupingBy(comment -> comment.getParent().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private Set<Long> extractRootParentIds(List<UserRecipeComment> parentComments) {
        if (parentComments == null || parentComments.isEmpty()) {
            return Set.of();
        }
        return parentComments.stream()
                .map(UserRecipeComment::getEffectiveRootParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private String buildCommentPath(Long savedCommentId, UserRecipeComment parent) {
        String currentSegment = formatCommentPathSegment(savedCommentId);
        if (parent == null) {
            return currentSegment;
        }
        String parentPath = resolveCommentPath(parent);
        if (parentPath == null || parentPath.isBlank()) {
            return currentSegment;
        }
        return parentPath + COMMENT_PATH_SEPARATOR + currentSegment;
    }

    private String resolveCommentPath(UserRecipeComment comment) {
        if (comment == null) {
            return null;
        }
        if (comment.getCommentPath() != null && !comment.getCommentPath().isBlank()) {
            return comment.getCommentPath();
        }
        Long commentId = comment.getId();
        if (comment.getParent() == null) {
            return formatCommentPathSegment(commentId);
        }
        String parentPath = resolveCommentPath(comment.getParent());
        String currentSegment = formatCommentPathSegment(commentId);
        if (parentPath == null || parentPath.isBlank()) {
            return currentSegment;
        }
        return parentPath + COMMENT_PATH_SEPARATOR + currentSegment;
    }

    private String formatCommentPathSegment(Long commentId) {
        if (commentId == null) {
            return "";
        }
        return String.format("%0" + COMMENT_PATH_SEGMENT_WIDTH + "d", commentId);
    }

    private boolean shouldDisplayComment(UserRecipeComment comment, Map<Long, List<UserRecipeComment>> repliesByParentId) {
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

    private boolean hasVisibleDescendants(Long commentId, Map<Long, List<UserRecipeComment>> repliesByParentId) {
        if (commentId == null) {
            return false;
        }
        List<UserRecipeComment> children = repliesByParentId.getOrDefault(commentId, List.of());
        for (UserRecipeComment child : children) {
            if (child.getState() == SoftDeleteState.ACTIVE) {
                return true;
            }
            if (child.getState() == SoftDeleteState.DELETED && hasVisibleDescendants(child.getId(), repliesByParentId)) {
                return true;
            }
        }
        return false;
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

    private UserRecipeCommentResponse convertToDto(UserRecipeComment comment) {
        return convertToDto(comment, Map.of());
    }

    private UserRecipeCommentResponse convertToDto(UserRecipeComment comment,
                                                   Map<Long, List<UserRecipeComment>> repliesByParentId) {
        return convertToDto(comment, repliesByParentId, comment.getRecipe().getId());
    }

    private UserRecipeCommentResponse convertToDto(UserRecipeComment comment,
                                                   Map<Long, List<UserRecipeComment>> repliesByParentId,
                                                   Long recipeId) {
        UserRecipeCommentResponse response = new UserRecipeCommentResponse();
        response.setId(comment.getId());
        response.setAccountId(comment.getAccount().getId());
        response.setName(comment.getAccount().getName());
        response.setProfileImageUrl(comment.getAccount().getProfileImageUrl());
        response.setRecipeId(recipeId);
        response.setContents(comment.getContents());
        response.setParentId(comment.getParent() != null ? comment.getParent().getId() : null);
        response.setState(comment.getState());
        response.setRegTime(comment.getRegTime());
        response.setUpdateTime(comment.getUpdateTime());

        List<UserRecipeCommentResponse> replies = repliesByParentId.getOrDefault(comment.getId(), List.of())
                .stream()
                .map(reply -> convertToDto(reply, repliesByParentId, recipeId))
                .toList();
        response.setReplies(replies);
        return response;
    }

    private record BlockedAccountFilter(boolean excludeBlocked, Set<Long> blockedAccountIds) {
    }
}
