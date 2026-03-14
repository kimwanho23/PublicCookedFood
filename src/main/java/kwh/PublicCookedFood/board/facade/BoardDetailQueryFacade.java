package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.board.application.query.view.BoardDetailPageView;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import kwh.PublicCookedFood.board.error.BoardErrorCode;
import kwh.PublicCookedFood.board.policy.BoardAuthorizationPolicy;
import kwh.PublicCookedFood.board.service.BoardCounterService;
import kwh.PublicCookedFood.board.service.BoardCounters;
import kwh.PublicCookedFood.board.service.BoardScrapService;
import kwh.PublicCookedFood.board.service.comment.CommentQueryService;
import kwh.PublicCookedFood.board.service.query.BoardDetailQueryService;
import kwh.PublicCookedFood.common.error.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BoardDetailQueryFacade {

    private final BoardDetailQueryService boardDetailQueryService;
    private final CommentQueryService commentQueryService;
    private final BoardAuthorizationPolicy boardAuthorizationPolicy;
    private final BoardCounterService boardCounterService;
    private final BoardInteractionStateResolver boardInteractionStateResolver;
    private final BoardDetailReadModelFactory boardDetailReadModelFactory;
    private final BoardScrapService boardScrapService;

    public BoardDetailPageView loadBoardDetail(Long boardId,
                                               Account account,
                                               Pageable pageable,
                                               boolean increaseViews) {
        BoardViewer viewer = BoardViewer.from(account);
        BoardDetailResponse board = boardDetailQueryService.getBoardDetail(boardId);
        if (boardAuthorizationPolicy.isViewRestricted(viewer, board.getAccountId())) {
            throw new AppException(BoardErrorCode.BOARD_VIEW_BLOCKED);
        }

        BoardInteractionState interactionState =
                boardInteractionStateResolver.resolve(boardId, viewer, board.getAccountId());
        BoardCounters counters = boardCounterService.getDetailCounters(
                boardId,
                interactionState.viewer(),
                increaseViews
        );
        BoardDetailResponse refreshedBoard = boardDetailReadModelFactory.applyCounters(board, counters);
        long scraps = boardScrapService.getScrapCount(boardId);

        org.springframework.data.domain.Page<kwh.PublicCookedFood.board.application.query.view.CommentNodeView> comments =
                commentQueryService.getCommentListWithReplies(boardId, pageable, interactionState.viewer());

        return boardDetailReadModelFactory.toViewData(
                refreshedBoard,
                counters,
                scraps,
                interactionState,
                comments
        );
    }
}
