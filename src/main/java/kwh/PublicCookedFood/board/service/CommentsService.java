package kwh.PublicCookedFood.board.service;

import org.springframework.transaction.annotation.Transactional;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.dto.request.CommentCreateRequest;
import kwh.PublicCookedFood.board.dto.response.CommentResponse;
import kwh.PublicCookedFood.board.error.BoardErrorCode;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.notification.service.NotificationService;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentsService {

    private final CommentsRepository commentsRepository;

    private final BoardRepository boardRepository;

    private final AccountRepository accountRepository;

    private final NotificationService notificationService;

    private final AccountBlockService accountBlockService;


    @Transactional
    public void deleteComment(Long id) {
        commentsRepository.updateState(id, SoftDeleteState.DELETED);
    }

    @Transactional
    public CommentResponse createComment(CommentCreateRequest commentsDto) {
        Comments parent = null;
        if (commentsDto.getParentId() != null) {
            parent = commentsRepository.findById(commentsDto.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid parent comment ID"));
            if (parent.getBoard() == null
                    || parent.getBoard().getId() == null
                    || !parent.getBoard().getId().equals(commentsDto.getBoardId())) {
                throw new IllegalArgumentException("부모 댓글 정보가 올바르지 않습니다.");
            }
            if (parent.getState() != SoftDeleteState.ACTIVE) {
                throw new IllegalArgumentException("삭제된 댓글에는 답글을 작성할 수 없습니다.");
            }
        }
        Account account = accountRepository.findById(commentsDto.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid account ID"));

        Board board = boardRepository.findByIdWithAccountAndState(commentsDto.getBoardId(), SoftDeleteState.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("삭제된 게시글에는 댓글을 작성할 수 없습니다."));
        if (board.getAccount() != null && accountBlockService.isEitherBlocked(account.getId(), board.getAccount().getId())) {
            throw new AppException(BoardErrorCode.BOARD_COMMENT_BLOCKED);
        }
        if (parent != null && parent.getAccount() != null && accountBlockService.isEitherBlocked(account.getId(), parent.getAccount().getId())) {
            throw new AppException(BoardErrorCode.BOARD_COMMENT_BLOCKED);
        }

        Comments comment = Comments.builder()
                .account(account)
                .board(board)
                .contents(commentsDto.getContents())
                .parent(parent)
                .state(commentsDto.getState() == null ? SoftDeleteState.ACTIVE : commentsDto.getState())
                .replies(new ArrayList<>())
                .build();

        Comments savedComment = commentsRepository.save(comment);
        notificationService.notifyOnNewComment(savedComment);
        return convertToDto(savedComment);
    }

    public Long getCommentsCount(Long id, Long viewerAccountId) {
        Set<Long> blockedAccountIds = accountBlockService.getViewRestrictedAccountIds(viewerAccountId);
        if (blockedAccountIds.isEmpty()) {
            return commentsRepository.countByBoardIdAndState(id, SoftDeleteState.ACTIVE);
        }
        return commentsRepository.countByBoardIdAndStateAndAccountIdNotIn(id, SoftDeleteState.ACTIVE, blockedAccountIds);
    }

    public Comments getComment(Long id) {
        return commentsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid comment ID"));
    }


    public Page<CommentResponse> getCommentListWithReplies(Long postId, Pageable pageable, Long viewerAccountId) {
        Set<Long> blockedAccountIds = accountBlockService.getViewRestrictedAccountIds(viewerAccountId);
        BlockedAccountFilter blockedAccountFilter = resolveBlockedAccountFilter(blockedAccountIds);
        boolean hasBlockedAccounts = blockedAccountFilter.excludeBlocked();

        Page<Comments> parentComments = commentsRepository
                .findParentCommentsWithAccountByBoardIdOrderByRegTimeAsc(
                        postId,
                        SoftDeleteState.ACTIVE,
                        SoftDeleteState.DELETED,
                        blockedAccountFilter.excludeBlocked(),
                        blockedAccountFilter.blockedAccountIds(),
                        pageable);

        List<Comments> replies = commentsRepository
                .findRepliesWithAccountAndParentByBoardIdOrderByRegTimeAsc(postId);

        if (hasBlockedAccounts) {
            replies = replies.stream()
                    .filter(reply -> !isBlockedAuthor(reply, blockedAccountIds))
                    .toList();
        }

        Map<Long, List<Comments>> repliesByParentId = replies.stream()
                .collect(Collectors.groupingBy(comment -> comment.getParent().getId(),
                        LinkedHashMap::new, Collectors.toList()));

        List<CommentResponse> content = parentComments.getContent().stream()
                .filter(comment -> !isBlockedAuthor(comment, blockedAccountIds))
                .filter(comment -> shouldDisplayComment(comment, repliesByParentId))
                .map(comment -> convertToDto(comment, repliesByParentId, postId))
                .toList();

        long visibleParentCount = commentsRepository.findParentCommentsByBoardId(
                        postId,
                        SoftDeleteState.ACTIVE,
                        SoftDeleteState.DELETED,
                        blockedAccountFilter.excludeBlocked(),
                        blockedAccountFilter.blockedAccountIds())
                .stream()
                .filter(comment -> shouldDisplayComment(comment, repliesByParentId))
                .count();

        return new PageImpl<>(content, pageable, visibleParentCount);
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
        if (comment == null || comment.getAccount() == null || comment.getAccount().getId() == null || blockedAccountIds.isEmpty()) {
            return false;
        }
        return blockedAccountIds.contains(comment.getAccount().getId());
    }

    public Page<Comments> getAccountCommentPage(Long accountId, Pageable pageable, Set<Long> blockedAccountIds) {
        if (accountId == null) {
            return Page.empty(pageable);
        }
        BlockedAccountFilter blockedAccountFilter = resolveBlockedAccountFilter(blockedAccountIds);
        return commentsRepository.findAccountCommentsWithBoard(
                accountId,
                SoftDeleteState.ACTIVE,
                SoftDeleteState.ACTIVE,
                blockedAccountFilter.excludeBlocked(),
                blockedAccountFilter.blockedAccountIds(),
                pageable
        );
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

    private CommentResponse convertToDto(Comments comment) {
        return convertToDto(comment, Map.of());
    }

    private CommentResponse convertToDto(Comments comment, Map<Long, List<Comments>> repliesByParentId) {
        return convertToDto(comment, repliesByParentId, comment.getBoard().getId());
    }

    private CommentResponse convertToDto(Comments comment, Map<Long, List<Comments>> repliesByParentId, Long boardId) {
        CommentResponse dto = new CommentResponse();
        dto.setId(comment.getId());
        dto.setAccountId(comment.getAccount().getId());
        dto.setName(comment.getAccount().getName());
        dto.setProfileImageUrl(comment.getAccount().getProfileImageUrl());
        dto.setBoardId(boardId);
        dto.setContents(comment.getContents());
        dto.setParentId(comment.getParent() != null ? comment.getParent().getId() : null);
        dto.setState(comment.getState());
        dto.setRegTime(comment.getRegTime());
        dto.setUpdateTime(comment.getUpdateTime());

        List<CommentResponse> replies = repliesByParentId.getOrDefault(comment.getId(), List.of())
                .stream()
                .map(reply -> convertToDto(reply, repliesByParentId, boardId))
                .toList();
        dto.setReplies(replies);

        return dto;
    }

}
