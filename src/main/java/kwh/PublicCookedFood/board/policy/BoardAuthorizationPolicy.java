package kwh.PublicCookedFood.board.policy;

import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.board.facade.BoardViewer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class BoardAuthorizationPolicy {

    private final AccountBlockService accountBlockService;

    public boolean canManageBoard(BoardViewer viewer, Long authorAccountId) {
        Objects.requireNonNull(viewer, "viewer");
        if (authorAccountId == null) {
            return false;
        }
        return viewer.maybeAccountId()
                .map(authorAccountId::equals)
                .orElse(false);
    }

    public boolean canManageBoard(long actorAccountId, Long authorAccountId) {
        if (authorAccountId == null) {
            return false;
        }
        return authorAccountId.equals(actorAccountId);
    }

    public boolean canManageComment(long actorAccountId, long boardId, Comments comment) {
        if (comment == null) {
            return false;
        }
        if (comment.getBoard() == null || comment.getBoard().getId() == null) {
            return false;
        }
        if (comment.getAccount() == null || comment.getAccount().getId() == null) {
            return false;
        }

        return comment.getBoard().getId().equals(boardId)
                && comment.getAccount().getId().equals(actorAccountId);
    }

    public boolean isViewRestricted(BoardViewer viewer, Long authorAccountId) {
        Objects.requireNonNull(viewer, "viewer");
        if (authorAccountId == null) {
            return false;
        }
        return viewer.maybeAccountId()
                .map(viewerAccountId -> accountBlockService.isEitherBlocked(viewerAccountId, authorAccountId))
                .orElse(false);
    }

    public boolean isAuthorBlockedByViewer(BoardViewer viewer, Long authorAccountId) {
        Objects.requireNonNull(viewer, "viewer");
        if (authorAccountId == null) {
            return false;
        }
        return viewer.maybeAccountId()
                .filter(viewerAccountId -> !viewerAccountId.equals(authorAccountId))
                .map(viewerAccountId -> accountBlockService.isBlocked(viewerAccountId, authorAccountId))
                .orElse(false);
    }
}
