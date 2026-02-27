package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.domain.BoardReportReason;
import kwh.PublicCookedFood.board.dto.request.BoardReportCreateRequest;
import kwh.PublicCookedFood.board.dto.request.CommentCreateRequest;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import kwh.PublicCookedFood.board.dto.response.CommentResponse;
import kwh.PublicCookedFood.user.domain.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BoardDetailFacade {

    private final BoardDetailQueryFacade boardDetailQueryFacade;
    private final BoardInteractionFacade boardInteractionFacade;

    public BoardDetailViewData loadBoardDetail(Long boardId,
                                               Users user,
                                               Pageable pageable) {
        return boardDetailQueryFacade.loadBoardDetail(boardId, user, pageable);
    }

    public void deleteComment(Users actor, Long boardId, Long commentId) {
        boardInteractionFacade.deleteComment(actor, boardId, commentId);
    }

    public OperationResult addComment(Users actor,
                                      Long boardId,
                                      CommentCreateRequest commentDto) {
        return boardInteractionFacade.addComment(actor, boardId, commentDto);
    }

    public void addScrap(Long boardId, Long actorUserId) {
        boardInteractionFacade.addScrap(boardId, actorUserId);
    }

    public void removeScrap(Long boardId, Long actorUserId) {
        boardInteractionFacade.removeScrap(boardId, actorUserId);
    }

    public OperationResult reportBoard(Long boardId,
                                       Long actorUserId,
                                       BoardReportCreateRequest reportDto) {
        return boardInteractionFacade.reportBoard(boardId, actorUserId, reportDto);
    }

    public void toggleLike(Long boardId, Long actorUserId) {
        boardInteractionFacade.toggleLike(boardId, actorUserId);
    }

    public record BoardDetailViewData(BoardDetailResponse boardDto,
                                      Long likes,
                                      long scraps,
                                      Boolean myLike,
                                      boolean myScrap,
                                      boolean myReport,
                                      boolean myBlockedAuthor,
                                      boolean boardInteractionBlocked,
                                      Long currentUserId,
                                      Page<CommentResponse> comments,
                                      Long commentsCount,
                                      BoardReportReason[] reportReasons) {
    }

    public record OperationResult(boolean success,
                                  String message) {

        public static OperationResult success(String message) {
            return new OperationResult(true, message);
        }

        public static OperationResult failure(String message) {
            return new OperationResult(false, message);
        }
    }
}
