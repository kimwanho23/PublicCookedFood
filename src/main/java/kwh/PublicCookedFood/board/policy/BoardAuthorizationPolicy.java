package kwh.PublicCookedFood.board.policy;

import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BoardAuthorizationPolicy {

    private final AccountBlockService accountBlockService;

    public boolean canManageBoard(Account actor, BoardDetailResponse board) {
        if (actor == null || actor.getId() == null || board == null || board.getAccountId() == null) {
            return false;
        }
        return board.getAccountId().equals(actor.getId());
    }

    public boolean canManageComment(Account actor, Long boardId, Comments comment) {
        if (actor == null || actor.getId() == null || boardId == null || comment == null) {
            return false;
        }
        if (comment.getBoard() == null || comment.getBoard().getId() == null) {
            return false;
        }
        if (comment.getAccount() == null || comment.getAccount().getId() == null) {
            return false;
        }

        return comment.getBoard().getId().equals(boardId)
                && comment.getAccount().getId().equals(actor.getId());
    }

    public boolean isViewRestricted(Account viewer, Long authorAccountId) {
        if (viewer == null || viewer.getId() == null || authorAccountId == null) {
            return false;
        }
        return accountBlockService.isEitherBlocked(viewer.getId(), authorAccountId);
    }

    public boolean isAuthorBlockedByViewer(Long viewerAccountId, Long authorAccountId) {
        if (viewerAccountId == null || authorAccountId == null || viewerAccountId.equals(authorAccountId)) {
            return false;
        }
        return accountBlockService.isBlocked(viewerAccountId, authorAccountId);
    }
}
