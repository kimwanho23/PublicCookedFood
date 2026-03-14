package kwh.PublicCookedFood.notification.service;

import kwh.PublicCookedFood.board.service.comment.CommentTargetPath;

import java.util.Collection;
import java.util.Map;

public interface NotificationCommentTargetPathService {

    Map<Long, CommentTargetPath> buildCommentTargetPaths(long boardId, Collection<Long> commentIds, long receiverId);

    CommentTargetPath buildCommentTargetPath(long boardId, long commentId, long receiverId);
}
