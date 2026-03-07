package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.domain.BoardReportReason;
import kwh.PublicCookedFood.board.dto.request.BoardReportCreateRequest;
import kwh.PublicCookedFood.board.dto.request.CommentCreateRequest;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import kwh.PublicCookedFood.board.dto.response.CommentResponse;
import kwh.PublicCookedFood.account.domain.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class BoardDetailFacade {

    private final BoardDetailQueryFacade boardDetailQueryFacade;
    private final BoardInteractionFacade boardInteractionFacade;

    public BoardDetailViewData loadBoardDetail(Long boardId,
                                               Account account,
                                               Pageable pageable,
                                               boolean increaseViews) {
        return boardDetailQueryFacade.loadBoardDetail(boardId, account, pageable, increaseViews);
    }

    public OperationResult deleteComment(Account actor, Long boardId, Long commentId) {
        return boardInteractionFacade.deleteComment(actor, boardId, commentId);
    }

    public OperationResult addComment(Account actor,
                                      Long boardId,
                                      CommentCreateRequest commentDto) {
        return boardInteractionFacade.addComment(actor, boardId, commentDto);
    }

    public void addScrap(Long boardId, Long actorAccountId) {
        boardInteractionFacade.addScrap(boardId, actorAccountId);
    }

    public void removeScrap(Long boardId, Long actorAccountId) {
        boardInteractionFacade.removeScrap(boardId, actorAccountId);
    }

    public OperationResult reportBoard(Long boardId,
                                       Long actorAccountId,
                                       BoardReportCreateRequest reportDto) {
        return boardInteractionFacade.reportBoard(boardId, actorAccountId, reportDto);
    }

    public void toggleLike(Long boardId, Long actorAccountId) {
        boardInteractionFacade.toggleLike(boardId, actorAccountId);
    }

    public record BoardDetailViewData(BoardDetailResponse boardDto,
                                      Long likes,
                                      long scraps,
                                      Boolean myLike,
                                      boolean myScrap,
                                      boolean myReport,
                                      boolean myBlockedAuthor,
                                      boolean boardInteractionBlocked,
                                      Long currentAccountId,
                                      Page<CommentResponse> comments,
                                      Long commentsCount,
                                      BoardReportReason[] reportReasons) {
        public BoardDetailViewData {
            reportReasons = reportReasons == null
                    ? new BoardReportReason[0]
                    : Arrays.copyOf(reportReasons, reportReasons.length);
        }

        @Override
        public BoardReportReason[] reportReasons() {
            return Arrays.copyOf(reportReasons, reportReasons.length);
        }
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
