package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.dto.request.BoardReportCreateRequest;
import kwh.PublicCookedFood.board.dto.request.CommentCreateRequest;
import kwh.PublicCookedFood.board.dto.response.CommentResponse;
import kwh.PublicCookedFood.board.policy.BoardAuthorizationPolicy;
import kwh.PublicCookedFood.board.service.BoardReportService;
import kwh.PublicCookedFood.board.service.BoardScrapService;
import kwh.PublicCookedFood.board.service.BoardService;
import kwh.PublicCookedFood.board.service.CommentNavigationService;
import kwh.PublicCookedFood.board.service.CommentsService;
import kwh.PublicCookedFood.board.service.LikeService;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.ErrorMessageResolver;
import kwh.PublicCookedFood.account.audit.BoardAuditPublisher;
import kwh.PublicCookedFood.account.domain.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class BoardInteractionFacade {

    private final CommentsService commentsService;
    private final BoardService boardService;
    private final BoardScrapService boardScrapService;
    private final BoardReportService boardReportService;
    private final LikeService likeService;
    private final BoardAuditPublisher boardAuditPublisher;
    private final BoardAuthorizationPolicy boardAuthorizationPolicy;
    private final CommentNavigationService commentNavigationService;

    @Transactional
    public BoardDetailFacade.OperationResult deleteComment(Account actor, Long boardId, Long commentId) {
        Comments comment = commentsService.getComment(commentId);
        if (!boardAuthorizationPolicy.canManageComment(actor, boardId, comment)) {
            return BoardDetailFacade.OperationResult.failure("댓글 삭제 권한이 없습니다.");
        }

        commentsService.deleteComment(commentId);
        boardService.updateCommentCounts(boardId);
        boardAuditPublisher.boardCommentDelete(actor.getId(), boardId, commentId);
        return BoardDetailFacade.OperationResult.success("댓글이 삭제되었습니다.");
    }

    @Transactional
    public BoardDetailFacade.OperationResult addComment(Account actor,
                                                        Long boardId,
                                                        CommentCreateRequest commentDto,
                                                        int commentSize) {
        try {
            commentDto.setAccountId(actor.getId());
            commentDto.setBoardId(boardId);
            CommentResponse savedComment = commentsService.createComment(commentDto);
            boardService.updateCommentCounts(boardId);
            boardAuditPublisher.boardCommentCreate(actor.getId(), boardId, savedComment.getId(), commentDto.getParentId());
            String redirectPath = commentNavigationService.buildCommentTargetPath(
                    boardId,
                    savedComment.getId(),
                    actor.getId(),
                    commentSize
            );
            return BoardDetailFacade.OperationResult.success(null, redirectPath);
        } catch (AppException | IllegalArgumentException e) {
            return failOperation(
                    e,
                    "댓글 등록에 실패했습니다.",
                    message -> boardAuditPublisher.boardCommentCreateFailed(actor.getId(), boardId, message)
            );
        }
    }

    @Transactional
    public void addScrap(Long boardId, Long actorAccountId) {
        boardScrapService.addScrap(boardId, actorAccountId);
        boardAuditPublisher.boardScrapAdd(actorAccountId, boardId);
    }

    @Transactional
    public void removeScrap(Long boardId, Long actorAccountId) {
        boardScrapService.removeScrap(boardId, actorAccountId);
        boardAuditPublisher.boardScrapRemove(actorAccountId, boardId);
    }

    @Transactional
    public BoardDetailFacade.OperationResult reportBoard(Long boardId,
                                                         Long actorAccountId,
                                                         BoardReportCreateRequest reportDto) {
        try {
            boardReportService.createReport(boardId, actorAccountId, reportDto.getReason(), reportDto.getDetails());
            boardAuditPublisher.boardReportCreate(actorAccountId, boardId, reportDto.getReason().name());
            return BoardDetailFacade.OperationResult.success("신고가 접수되었습니다.");
        } catch (AppException | IllegalArgumentException e) {
            return failOperation(
                    e,
                    "신고 처리에 실패했습니다.",
                    message -> boardAuditPublisher.boardReportCreateFailed(actorAccountId, boardId, message)
            );
        }
    }

    @Transactional
    public void toggleLike(Long boardId, Long actorAccountId) {
        likeService.saveLikes(boardId, actorAccountId);
        boardService.updateLikes(boardId);
        boardAuditPublisher.boardLikeToggle(actorAccountId, boardId);
    }

    private BoardDetailFacade.OperationResult failOperation(RuntimeException e,
                                                            String fallbackMessage,
                                                            Consumer<String> failureAudit) {
        String message = ErrorMessageResolver.resolve(e, fallbackMessage);
        failureAudit.accept(message);
        return BoardDetailFacade.OperationResult.failure(message);
    }
}
