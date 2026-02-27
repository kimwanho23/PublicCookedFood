package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.dto.request.BoardSaveRequest;
import kwh.PublicCookedFood.board.dto.request.BoardUpdateRequest;
import kwh.PublicCookedFood.board.dto.request.BoardWriteRequest;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import kwh.PublicCookedFood.board.service.BoardService;
import kwh.PublicCookedFood.notification.service.NotificationService;
import kwh.PublicCookedFood.user.audit.BoardAuditPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardCommandFacade {

    private final BoardService boardService;
    private final NotificationService notificationService;
    private final BoardAuditPublisher boardAuditPublisher;

    @Transactional
    public Board createBoard(Long actorUserId, BoardWriteRequest boardDto) {
        BoardSaveRequest command = BoardSaveRequest.forCreate(boardDto, actorUserId);
        Board savedBoard = boardService.save(command);
        notificationService.notifyOnBoardCreated(savedBoard);
        boardAuditPublisher.boardCreate(actorUserId, savedBoard.getId());
        return savedBoard;
    }

    @Transactional
    public void updateBoard(Long actorUserId,
                            Long boardId,
                            BoardUpdateRequest boardDto,
                            BoardDetailResponse existingBoard) {
        BoardSaveRequest command = BoardSaveRequest.forUpdate(boardDto, existingBoard);
        boardService.save(command);
        if (actorUserId != null) {
            boardAuditPublisher.boardUpdate(actorUserId, boardId);
        }
    }

    @Transactional
    public void deleteBoard(Long actorUserId, Long boardId) {
        boardService.delete(boardId);
        if (actorUserId != null) {
            boardAuditPublisher.boardDelete(actorUserId, boardId);
        }
    }
}
