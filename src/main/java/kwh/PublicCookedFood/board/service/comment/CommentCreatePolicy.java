package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.error.BoardErrorCode;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
    @RequiredArgsConstructor
public class CommentCreatePolicy {

    private final AccountBlockService accountBlockService;

    public void validateParent(ResolvedCommentParent parent, long boardId) {
        parent.validateParent(boardId);
    }

    public void validateActorVisibility(Account actor, Board board, ResolvedCommentParent parent) {
        if (board.getAccount() != null && accountBlockService.isEitherBlocked(actor.getId(), board.getAccount().getId())) {
            throw new AppException(BoardErrorCode.BOARD_COMMENT_BLOCKED);
        }
        parent.validateReplyActorVisibility(actor.getId(), accountBlockService);
    }
}
