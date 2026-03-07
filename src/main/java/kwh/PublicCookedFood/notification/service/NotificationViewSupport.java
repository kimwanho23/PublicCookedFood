package kwh.PublicCookedFood.notification.service;

import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.service.CommentNavigationService;
import kwh.PublicCookedFood.notification.domain.Notification;
import kwh.PublicCookedFood.notification.domain.NotificationType;
import kwh.PublicCookedFood.notification.dto.response.NotificationResponse;
import kwh.PublicCookedFood.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificationViewSupport {

    private static final Set<NotificationType> REPORT_RESULT_TYPES = Set.of(
            NotificationType.REPORT_RESOLVED,
            NotificationType.REPORT_REJECTED
    );

    private final NotificationRepository notificationRepository;
    private final AccountBlockService accountBlockService;
    private final CommentNavigationService commentNavigationService;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> loadVisibleNotificationPage(Long receiverId,
                                                                  Pageable pageable,
                                                                  boolean unreadOnly) {
        if (receiverId == null) {
            return Page.empty(pageable == null ? Pageable.unpaged() : pageable);
        }

        NotificationVisibility visibility = resolveVisibility(receiverId);
        Page<Notification> notifications = notificationRepository.findVisibleNotificationsByReceiverId(
                receiverId,
                unreadOnly,
                SoftDeleteState.ACTIVE,
                REPORT_RESULT_TYPES,
                visibility.excludeRestricted(),
                visibility.restrictedAccountIdsOrSentinel(),
                pageable
        );

        Map<Long, Map<Long, String>> commentTargetPaths = buildCommentTargetPathsByBoard(notifications.getContent(), receiverId);
        return notifications.map(notification -> toResponse(notification, receiverId, commentTargetPaths));
    }

    @Transactional(readOnly = true)
    public long countVisibleUnreadNotifications(Long receiverId) {
        if (receiverId == null) {
            return 0L;
        }
        NotificationVisibility visibility = resolveVisibility(receiverId);
        return notificationRepository.countVisibleUnreadNotificationsByReceiverId(
                receiverId,
                SoftDeleteState.ACTIVE,
                REPORT_RESULT_TYPES,
                visibility.excludeRestricted(),
                visibility.restrictedAccountIdsOrSentinel()
        );
    }

    @Transactional(readOnly = true)
    public NotificationResponse loadVisibleNotification(Long receiverId, Long notificationId) {
        if (receiverId == null || notificationId == null) {
            return null;
        }
        NotificationVisibility visibility = resolveVisibility(receiverId);
        return notificationRepository.findWithActorBoardAndCommentById(notificationId)
                .filter(notification -> isVisibleToReceiver(notification, receiverId, visibility.restrictedAccountIds()))
                .map(notification -> toResponse(notification, receiverId, Map.of()))
                .orElse(null);
    }

    private NotificationVisibility resolveVisibility(Long receiverId) {
        Set<Long> restrictedAccountIds = accountBlockService.getViewRestrictedAccountIds(receiverId);
        if (restrictedAccountIds == null || restrictedAccountIds.isEmpty()) {
            return new NotificationVisibility(false, Set.of());
        }
        return new NotificationVisibility(true, Set.copyOf(restrictedAccountIds));
    }

    private boolean isVisibleToReceiver(Notification notification,
                                        Long receiverId,
                                        Set<Long> restrictedAccountIds) {
        if (notification == null
                || notification.getReceiver() == null
                || notification.getReceiver().getId() == null
                || !notification.getReceiver().getId().equals(receiverId)) {
            return false;
        }

        Board board = notification.getBoard();
        if (board == null
                || board.getId() == null
                || board.getState() != SoftDeleteState.ACTIVE) {
            return false;
        }

        if (board.isHiddenByReport() && !isReportResultNotification(notification)) {
            return false;
        }

        if (restrictedAccountIds == null || restrictedAccountIds.isEmpty()) {
            return true;
        }

        Long actorId = notification.getActor() == null ? null : notification.getActor().getId();
        if (actorId != null && restrictedAccountIds.contains(actorId)) {
            return false;
        }

        Long boardOwnerId = board.getAccount() == null ? null : board.getAccount().getId();
        return boardOwnerId == null || !restrictedAccountIds.contains(boardOwnerId);
    }

    private NotificationResponse toResponse(Notification notification,
                                            Long receiverId,
                                            Map<Long, Map<Long, String>> commentTargetPaths) {
        String targetPath = buildTargetPath(notification, receiverId, commentTargetPaths);
        return NotificationResponse.from(notification, targetPath);
    }

    private String buildTargetPath(Notification notification,
                                   Long receiverId,
                                   Map<Long, Map<Long, String>> commentTargetPaths) {
        if (notification == null || notification.getBoard() == null || notification.getBoard().getId() == null) {
            return "/boards";
        }

        if (notification.getBoard().isHiddenByReport() && isReportResultNotification(notification)) {
            return "/boards";
        }

        Long boardId = notification.getBoard().getId();
        if (notification.getComment() == null || notification.getComment().getId() == null) {
            return "/boards/" + boardId;
        }

        Long commentId = notification.getComment().getId();
        String precomputedPath = commentTargetPaths.getOrDefault(boardId, Map.of()).get(commentId);
        if (precomputedPath != null && !precomputedPath.isBlank()) {
            return precomputedPath;
        }

        return commentNavigationService.buildCommentTargetPath(
                boardId,
                commentId,
                receiverId
        );
    }

    private boolean isReportResultNotification(Notification notification) {
        if (notification == null || notification.getType() == null) {
            return false;
        }
        NotificationType type = notification.getType();
        return type == NotificationType.REPORT_RESOLVED || type == NotificationType.REPORT_REJECTED;
    }

    private Map<Long, Map<Long, String>> buildCommentTargetPathsByBoard(Collection<Notification> notifications,
                                                                        Long receiverId) {
        if (notifications == null || notifications.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<Long>> commentIdsByBoard = notifications.stream()
                .filter(notification -> notification != null
                        && notification.getBoard() != null
                        && notification.getBoard().getId() != null
                        && notification.getComment() != null
                        && notification.getComment().getId() != null)
                .collect(Collectors.groupingBy(notification -> notification.getBoard().getId(),
                        Collectors.mapping(notification -> notification.getComment().getId(), Collectors.toList())));

        if (commentIdsByBoard.isEmpty()) {
            return Map.of();
        }

        return commentIdsByBoard.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> commentNavigationService.buildCommentTargetPaths(
                                entry.getKey(),
                                entry.getValue(),
                                receiverId
                        )
                ));
    }

    private record NotificationVisibility(boolean excludeRestricted, Set<Long> restrictedAccountIds) {

        private List<Long> restrictedAccountIdsOrSentinel() {
            if (restrictedAccountIds == null || restrictedAccountIds.isEmpty()) {
                return List.of(-1L);
            }
            return List.copyOf(restrictedAccountIds);
        }
    }
}
