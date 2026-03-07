package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.domain.BoardReportReason;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import kwh.PublicCookedFood.board.dto.response.CommentResponse;
import kwh.PublicCookedFood.board.error.BoardErrorCode;
import kwh.PublicCookedFood.board.policy.BoardAuthorizationPolicy;
import kwh.PublicCookedFood.board.service.BoardReportService;
import kwh.PublicCookedFood.board.service.BoardScrapService;
import kwh.PublicCookedFood.board.service.BoardService;
import kwh.PublicCookedFood.board.service.CommentsService;
import kwh.PublicCookedFood.board.service.LikeService;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.metrics.view.ViewCounterService;
import kwh.PublicCookedFood.account.domain.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BoardDetailQueryFacade {

    private final BoardService boardService;
    private final CommentsService commentsService;
    private final LikeService likeService;
    private final BoardScrapService boardScrapService;
    private final BoardReportService boardReportService;
    private final BoardAuthorizationPolicy boardAuthorizationPolicy;
    private final ViewCounterService viewCounterService;

    public BoardDetailFacade.BoardDetailViewData loadBoardDetail(Long boardId,
                                                                 Account account,
                                                                 Pageable pageable,
                                                                 boolean increaseViews) {
        BoardDetailResponse board = boardService.getBoardDetail(boardId);
        if (boardAuthorizationPolicy.isViewRestricted(account, board.getAccountId())) {
            throw new AppException(BoardErrorCode.BOARD_VIEW_BLOCKED);
        }

        long currentViews = increaseViews
                ? viewCounterService.increaseBoardViewAndGet(boardId)
                : viewCounterService.getBoardViewCount(boardId);
        BoardDetailResponse refreshedBoard = copyWithViews(board, currentViews);
        Long likes = likeService.getLike(boardId);
        long scraps = boardScrapService.getScrapCount(boardId);

        Long currentAccountId = account == null ? null : account.getId();
        Boolean myLike = null;
        boolean myScrap = false;
        boolean myReport = false;
        boolean myBlockedAuthor = false;
        boolean boardInteractionBlocked = false;

        if (currentAccountId != null) {
            myLike = likeService.findMyLike(boardId, currentAccountId);
            myScrap = boardScrapService.isScrapped(boardId, currentAccountId);
            myReport = boardReportService.hasReported(boardId, currentAccountId);
            myBlockedAuthor = boardAuthorizationPolicy.isAuthorBlockedByViewer(currentAccountId, refreshedBoard.getAccountId());
            boardInteractionBlocked = myBlockedAuthor;
        }

        Page<CommentResponse> comments = commentsService.getCommentListWithReplies(boardId, pageable, currentAccountId);
        Long commentsCount = commentsService.getCommentsCount(boardId, currentAccountId);
        comments.forEach(comment -> comment.setAreAllRepliesDeleted(comment.areAllRepliesDeleted()));

        return new BoardDetailFacade.BoardDetailViewData(
                refreshedBoard,
                likes,
                scraps,
                myLike,
                myScrap,
                myReport,
                myBlockedAuthor,
                boardInteractionBlocked,
                currentAccountId,
                comments,
                commentsCount,
                BoardReportReason.values()
        );
    }

    private BoardDetailResponse copyWithViews(BoardDetailResponse source, long views) {
        return BoardDetailResponse.builder()
                .id(source.getId())
                .title(source.getTitle())
                .contents(source.getContents())
                .accountId(source.getAccountId())
                .accountName(source.getAccountName())
                .accountProfileImageUrl(source.getAccountProfileImageUrl())
                .sectionId(source.getSectionId())
                .sectionKey(source.getSectionKey())
                .sectionName(source.getSectionName())
                .views(views)
                .likesCount(source.getLikesCount())
                .commentsCount(source.getCommentsCount())
                .state(source.getState())
                .regTime(source.getRegTime())
                .updateTime(source.getUpdateTime())
                .build();
    }
}
