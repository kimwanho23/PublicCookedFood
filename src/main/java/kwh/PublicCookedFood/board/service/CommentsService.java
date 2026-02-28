package kwh.PublicCookedFood.board.service;

import org.springframework.transaction.annotation.Transactional;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.dto.request.CommentCreateRequest;
import kwh.PublicCookedFood.board.dto.response.CommentResponse;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.notification.service.NotificationService;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.repository.UserRepository;
import kwh.PublicCookedFood.user.service.UserBlockService;
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

    private final UserRepository userRepository;

    private final NotificationService notificationService;

    private final UserBlockService userBlockService;


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
        Users user = userRepository.findById(commentsDto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID"));

        Board board = boardRepository.findById(commentsDto.getBoardId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid board ID"));
        if (board.getState() != SoftDeleteState.ACTIVE) {
            throw new IllegalArgumentException("삭제된 게시글에는 댓글을 작성할 수 없습니다.");
        }
        if (board.getUser() != null && userBlockService.isEitherBlocked(user.getId(), board.getUser().getId())) {
            throw new IllegalStateException("차단 관계인 사용자에게는 댓글을 작성할 수 없습니다.");
        }
        if (parent != null && parent.getUser() != null && userBlockService.isEitherBlocked(user.getId(), parent.getUser().getId())) {
            throw new IllegalStateException("차단 관계인 사용자에게는 댓글을 작성할 수 없습니다.");
        }

        Comments comment = Comments.builder()
                .user(user)
                .board(board)
                .contents(commentsDto.getContents())
                .parent(parent)
                .state(commentsDto.getState() == null ? SoftDeleteState.ACTIVE : commentsDto.getState())
                .replies(new ArrayList<>()) // 대댓글 초기화
                .build();

        Comments savedComment = commentsRepository.save(comment);
        notificationService.notifyOnNewComment(savedComment);
        return convertToDto(savedComment);
    }

    public Long getCommentsCount(Long id, Long viewerUserId) {
        Set<Long> blockedUserIds = userBlockService.getViewRestrictedUserIds(viewerUserId);
        if (blockedUserIds.isEmpty()) {
            return commentsRepository.countByBoardIdAndState(id, SoftDeleteState.ACTIVE);
        }
        return commentsRepository.countByBoardIdAndStateAndUserIdNotIn(id, SoftDeleteState.ACTIVE, blockedUserIds);
    }

    public Comments getComment(Long id) {
        return commentsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid comment ID"));
    }


    public Page<CommentResponse> getCommentListWithReplies(Long postId, Pageable pageable, Long viewerUserId) {
        Set<Long> blockedUserIds = userBlockService.getViewRestrictedUserIds(viewerUserId);
        BlockedUserFilter blockedUserFilter = resolveBlockedUserFilter(blockedUserIds);
        boolean hasBlockedUsers = blockedUserFilter.excludeBlocked();

        Page<Comments> parentComments = commentsRepository
                .findParentCommentsWithUserByBoardIdOrderByRegTimeAsc(
                        postId,
                        SoftDeleteState.ACTIVE,
                        SoftDeleteState.DELETED,
                        blockedUserFilter.excludeBlocked(),
                        blockedUserFilter.blockedUserIds(),
                        pageable);

        List<Comments> replies = commentsRepository
                .findRepliesWithUserAndParentByBoardIdOrderByRegTimeAsc(postId);

        if (hasBlockedUsers) {
            replies = replies.stream()
                    .filter(reply -> !isBlockedAuthor(reply, blockedUserIds))
                    .toList();
        }

        Map<Long, List<Comments>> repliesByParentId = replies.stream()
                .collect(Collectors.groupingBy(comment -> comment.getParent().getId(),
                        LinkedHashMap::new, Collectors.toList()));

        List<CommentResponse> content = parentComments.getContent().stream()
                .filter(comment -> !isBlockedAuthor(comment, blockedUserIds))
                .filter(comment -> shouldDisplayComment(comment, repliesByParentId))
                .map(comment -> convertToDto(comment, repliesByParentId, postId))
                .toList();

        long visibleParentCount = commentsRepository.findParentCommentsByBoardId(
                        postId,
                        SoftDeleteState.ACTIVE,
                        SoftDeleteState.DELETED,
                        blockedUserFilter.excludeBlocked(),
                        blockedUserFilter.blockedUserIds())
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

    private boolean isBlockedAuthor(Comments comment, Set<Long> blockedUserIds) {
        if (comment == null || comment.getUser() == null || comment.getUser().getId() == null || blockedUserIds.isEmpty()) {
            return false;
        }
        return blockedUserIds.contains(comment.getUser().getId());
    }

    public Page<Comments> getUserCommentPage(Long userId, Pageable pageable, Set<Long> blockedUserIds) {
        if (userId == null) {
            return Page.empty(pageable);
        }
        BlockedUserFilter blockedUserFilter = resolveBlockedUserFilter(blockedUserIds);
        return commentsRepository.findUserCommentsWithBoard(
                userId,
                SoftDeleteState.ACTIVE,
                SoftDeleteState.ACTIVE,
                blockedUserFilter.excludeBlocked(),
                blockedUserFilter.blockedUserIds(),
                pageable
        );
    }

    private BlockedUserFilter resolveBlockedUserFilter(Set<Long> blockedUserIds) {
        if (blockedUserIds == null || blockedUserIds.isEmpty()) {
            return new BlockedUserFilter(false, Set.of(-1L));
        }
        Set<Long> normalized = blockedUserIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (normalized.isEmpty()) {
            return new BlockedUserFilter(false, Set.of(-1L));
        }
        return new BlockedUserFilter(true, normalized);
    }

    private record BlockedUserFilter(boolean excludeBlocked, Set<Long> blockedUserIds) {
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
        dto.setUserId(comment.getUser().getId());
        dto.setName(comment.getUser().getName());
        dto.setProfileImageUrl(comment.getUser().getProfileImageUrl());
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
