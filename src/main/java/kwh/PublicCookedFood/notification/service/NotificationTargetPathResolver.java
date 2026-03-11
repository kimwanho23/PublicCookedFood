package kwh.PublicCookedFood.notification.service;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.service.CommentNavigationService;
import kwh.PublicCookedFood.notification.domain.BoardNotificationTarget;
import kwh.PublicCookedFood.notification.domain.CommentNotificationTarget;
import kwh.PublicCookedFood.notification.domain.Notification;
import kwh.PublicCookedFood.notification.domain.NotificationTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationTargetPathResolver {

    private final CommentNavigationService commentNavigationService;

    public Map<Long, Map<Long, String>> precomputePaths(Collection<Notification> notifications, long receiverId) {
        CommentTargetRequests requests = CommentTargetRequests.from(notifications);
        if (requests.isEmpty()) {
            return Map.of();
        }

        LinkedHashMap<Long, Map<Long, String>> targetPathsByBoardId = new LinkedHashMap<>();
        for (CommentTargetRequest request : requests.values()) {
            targetPathsByBoardId.put(
                    request.boardId(),
                    commentNavigationService.buildCommentTargetPaths(request.boardId(), request.commentIds(), receiverId)
            );
        }
        return Map.copyOf(targetPathsByBoardId);
    }

    public String resolveTargetPath(Notification notification,
                                    long receiverId,
                                    Map<Long, Map<Long, String>> commentTargetPaths) {
        Board board = notification.getBoard();
        if (board.isHiddenByReport() && notification.isReportResult()) {
            return "/boards";
        }

        NotificationTarget target = notification.target();
        if (target instanceof BoardNotificationTarget boardTarget) {
            return "/boards/" + boardTarget.boardId();
        }
        CommentNotificationTarget commentTarget = (CommentNotificationTarget) target;

        String precomputedPath = commentTargetPaths
                .getOrDefault(commentTarget.boardId(), Map.of())
                .get(commentTarget.commentId());
        if (precomputedPath != null && !precomputedPath.isBlank()) {
            return precomputedPath;
        }
        return commentNavigationService.buildCommentTargetPath(
                commentTarget.boardId(),
                commentTarget.commentId(),
                receiverId
        );
    }

    private record CommentTargetRequests(List<CommentTargetRequest> values) {

        private static final CommentTargetRequests EMPTY = new CommentTargetRequests(List.of());

        private static CommentTargetRequests from(Iterable<Notification> notifications) {
            LinkedHashMap<Long, List<Long>> commentIdsByBoard = new LinkedHashMap<>();
            for (Notification notification : notifications) {
                if (!(notification.target() instanceof CommentNotificationTarget commentTarget)) {
                    continue;
                }
                commentIdsByBoard
                        .computeIfAbsent(commentTarget.boardId(), ignored -> new ArrayList<>())
                        .add(commentTarget.commentId());
            }
            if (commentIdsByBoard.isEmpty()) {
                return EMPTY;
            }
            List<CommentTargetRequest> requests = new ArrayList<>(commentIdsByBoard.size());
            commentIdsByBoard.forEach((boardId, commentIds) -> requests.add(new CommentTargetRequest(boardId, commentIds)));
            return new CommentTargetRequests(requests);
        }

        private boolean isEmpty() {
            return values.isEmpty();
        }
    }

    private record CommentTargetRequest(long boardId, List<Long> commentIds) {
    }
}
