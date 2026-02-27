package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.dto.request.BoardReportCreateRequest;
import kwh.PublicCookedFood.board.dto.request.CommentCreateRequest;
import kwh.PublicCookedFood.board.dto.response.CommentResponse;
import kwh.PublicCookedFood.board.policy.BoardAuthorizationPolicy;
import kwh.PublicCookedFood.board.service.BoardReportService;
import kwh.PublicCookedFood.board.service.BoardScrapService;
import kwh.PublicCookedFood.board.service.BoardService;
import kwh.PublicCookedFood.board.service.CommentsService;
import kwh.PublicCookedFood.board.service.LikeService;
import kwh.PublicCookedFood.user.audit.BoardAuditPublisher;
import kwh.PublicCookedFood.user.domain.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public void deleteComment(Users actor, Long boardId, Long commentId) {
        Comments comment = commentsService.getComment(commentId);
        if (!boardAuthorizationPolicy.canManageComment(actor, boardId, comment)) {
            return;
        }

        commentsService.deleteComment(commentId);
        boardService.updateCommentCounts(boardId);
        boardAuditPublisher.boardCommentDelete(actor.getId(), boardId, commentId);
    }

    @Transactional
    public BoardDetailFacade.OperationResult addComment(Users actor,
                                                        Long boardId,
                                                        CommentCreateRequest commentDto) {
        try {
            commentDto.setUserId(actor.getId());
            commentDto.setBoardId(boardId);
            CommentResponse savedComment = commentsService.createComment(commentDto);
            boardService.updateCommentCounts(boardId);
            boardAuditPublisher.boardCommentCreate(actor.getId(), boardId, savedComment.getId(), commentDto.getParentId());
            return BoardDetailFacade.OperationResult.success(null);
        } catch (IllegalArgumentException | IllegalStateException e) {
            boardAuditPublisher.boardCommentCreateFailed(actor.getId(), boardId, e.getMessage());
            return BoardDetailFacade.OperationResult.failure(e.getMessage());
        }
    }

    @Transactional
    public void addScrap(Long boardId, Long actorUserId) {
        boardScrapService.addScrap(boardId, actorUserId);
        boardAuditPublisher.boardScrapAdd(actorUserId, boardId);
    }

    @Transactional
    public void removeScrap(Long boardId, Long actorUserId) {
        boardScrapService.removeScrap(boardId, actorUserId);
        boardAuditPublisher.boardScrapRemove(actorUserId, boardId);
    }

    @Transactional
    public BoardDetailFacade.OperationResult reportBoard(Long boardId,
                                                         Long actorUserId,
                                                         BoardReportCreateRequest reportDto) {
        try {
            boardReportService.createReport(boardId, actorUserId, reportDto.getReason(), reportDto.getDetails());
            boardAuditPublisher.boardReportCreate(actorUserId, boardId, reportDto.getReason().name());
            return BoardDetailFacade.OperationResult.success("신고가 접수되었습니다.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            boardAuditPublisher.boardReportCreateFailed(actorUserId, boardId, e.getMessage());
            return BoardDetailFacade.OperationResult.failure(e.getMessage());
        }
    }

    @Transactional
    public void toggleLike(Long boardId, Long actorUserId) {
        likeService.saveLikes(boardId, actorUserId);
        boardService.updateLikes(boardId);
        boardAuditPublisher.boardLikeToggle(actorUserId, boardId);
    }
}
