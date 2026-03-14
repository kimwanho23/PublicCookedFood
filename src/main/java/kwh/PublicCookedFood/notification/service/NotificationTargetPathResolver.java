package kwh.PublicCookedFood.notification.service;

import kwh.PublicCookedFood.board.service.comment.CommentTargetPath;
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

    private final NotificationCommentTargetPathService notificationCommentTargetPathService;

    public CommentTargetPathIndex precomputePaths(Collection<Notification> notifications, long receiverId) {
        CommentTargetRequests requests = CommentTargetRequests.from(notifications);
        if (requests.isEmpty()) {
            return CommentTargetPathIndex.empty();
        }

        LinkedHashMap<CommentTargetKey, CommentTargetPath> targetPaths = new LinkedHashMap<>();
        for (CommentTargetRequest request : requests.values()) {
            Map<Long, CommentTargetPath> resolvedPaths = notificationCommentTargetPathService.buildCommentTargetPaths(
                    request.boardId(),
                    request.commentIds(),
                    receiverId
            );
            resolvedPaths.forEach((commentId, targetPath) ->
                    targetPaths.put(CommentTargetKey.of(request.boardId(), commentId), targetPath));
        }
        return CommentTargetPathIndex.of(targetPaths);
    }

    public CommentTargetPath resolveTargetPath(Notification notification,
                                               long receiverId,
                                               CommentTargetPathIndex commentTargetPaths) {
        if (notification.getBoard().isHiddenByReport() && notification.isReportResult()) {
            return CommentTargetPath.of("/boards");
        }

        NotificationTarget target = notification.target();
        if (target instanceof BoardNotificationTarget boardTarget) {
            return CommentTargetPath.of("/boards/" + boardTarget.boardId());
        }
        CommentNotificationTarget commentTarget = (CommentNotificationTarget) target;

        return commentTargetPaths.find(commentTarget.boardId(), commentTarget.commentId())
                .orElseGet(() -> notificationCommentTargetPathService.buildCommentTargetPath(
                        commentTarget.boardId(),
                        commentTarget.commentId(),
                        receiverId
                ));
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
