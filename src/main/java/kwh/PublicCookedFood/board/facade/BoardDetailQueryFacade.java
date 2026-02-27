package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.domain.BoardReportReason;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import kwh.PublicCookedFood.board.dto.response.CommentResponse;
import kwh.PublicCookedFood.board.policy.BoardAuthorizationPolicy;
import kwh.PublicCookedFood.board.service.BoardReportService;
import kwh.PublicCookedFood.board.service.BoardScrapService;
import kwh.PublicCookedFood.board.service.BoardService;
import kwh.PublicCookedFood.board.service.CommentsService;
import kwh.PublicCookedFood.board.service.LikeService;
import kwh.PublicCookedFood.user.domain.Users;
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

    public BoardDetailFacade.BoardDetailViewData loadBoardDetail(Long boardId,
                                                                 Users user,
                                                                 Pageable pageable) {
        BoardDetailResponse board = boardService.getBoardDetail(boardId);
        if (boardAuthorizationPolicy.isViewRestricted(user, board.getUserId())) {
            throw new IllegalStateException("차단 관계인 사용자의 게시글은 조회할 수 없습니다.");
        }

        boardService.updateViews(boardId);
        BoardDetailResponse refreshedBoard = boardService.getBoardDetail(boardId);
        Long likes = likeService.getLike(boardId);
        long scraps = boardScrapService.getScrapCount(boardId);

        Long currentUserId = user == null ? null : user.getId();
        Boolean myLike = null;
        boolean myScrap = false;
        boolean myReport = false;
        boolean myBlockedAuthor = false;
        boolean boardInteractionBlocked = false;

        if (currentUserId != null) {
            myLike = likeService.findMyLike(boardId, currentUserId);
            myScrap = boardScrapService.isScrapped(boardId, currentUserId);
            myReport = boardReportService.hasReported(boardId, currentUserId);
            myBlockedAuthor = boardAuthorizationPolicy.isAuthorBlockedByViewer(currentUserId, refreshedBoard.getUserId());
            boardInteractionBlocked = myBlockedAuthor;
        }

        Page<CommentResponse> comments = commentsService.getCommentListWithReplies(boardId, pageable, currentUserId);
        Long commentsCount = commentsService.getCommentsCount(boardId, currentUserId);
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
                currentUserId,
                comments,
                commentsCount,
                BoardReportReason.values()
        );
    }
}
