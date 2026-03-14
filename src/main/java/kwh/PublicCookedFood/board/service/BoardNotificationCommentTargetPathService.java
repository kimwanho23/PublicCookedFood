package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.board.service.comment.CommentTargetPathQuery;
import kwh.PublicCookedFood.board.service.comment.CommentTargetPathsQuery;
import kwh.PublicCookedFood.board.service.comment.CommentTargetPath;
import kwh.PublicCookedFood.board.facade.BoardViewer;
import kwh.PublicCookedFood.notification.service.NotificationCommentTargetPathService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BoardNotificationCommentTargetPathService implements NotificationCommentTargetPathService {

    private final CommentNavigationService commentNavigationService;

    @Override
    public Map<Long, CommentTargetPath> buildCommentTargetPaths(long boardId, Collection<Long> commentIds, long receiverId) {
        return commentNavigationService.buildCommentTargetPaths(
                CommentTargetPathsQuery.of(boardId, commentIds, BoardViewer.authenticated(receiverId))
        );
    }

    @Override
    public CommentTargetPath buildCommentTargetPath(long boardId, long commentId, long receiverId) {
        return commentNavigationService.buildCommentTargetPath(
                CommentTargetPathQuery.of(boardId, commentId, BoardViewer.authenticated(receiverId))
        );
    }
}
