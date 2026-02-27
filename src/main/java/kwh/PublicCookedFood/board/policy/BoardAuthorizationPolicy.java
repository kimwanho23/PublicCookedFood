package kwh.PublicCookedFood.board.policy;

import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.service.UserBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BoardAuthorizationPolicy {

    private final UserBlockService userBlockService;

    public boolean canManageBoard(Users actor, BoardDetailResponse board) {
        if (actor == null || actor.getId() == null || board == null || board.getUserId() == null) {
            return false;
        }
        return board.getUserId().equals(actor.getId());
    }

    public boolean canManageComment(Users actor, Long boardId, Comments comment) {
        if (actor == null || actor.getId() == null || boardId == null || comment == null) {
            return false;
        }
        if (comment.getBoard() == null || comment.getBoard().getId() == null) {
            return false;
        }
        if (comment.getUser() == null || comment.getUser().getId() == null) {
            return false;
        }

        return comment.getBoard().getId().equals(boardId)
                && comment.getUser().getId().equals(actor.getId());
    }

    public boolean isViewRestricted(Users viewer, Long authorUserId) {
        if (viewer == null || viewer.getId() == null || authorUserId == null) {
            return false;
        }
        return userBlockService.isEitherBlocked(viewer.getId(), authorUserId);
    }

    public boolean isAuthorBlockedByViewer(Long viewerUserId, Long authorUserId) {
        if (viewerUserId == null || authorUserId == null || viewerUserId.equals(authorUserId)) {
            return false;
        }
        return userBlockService.isBlocked(viewerUserId, authorUserId);
    }
}
