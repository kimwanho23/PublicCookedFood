package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.dto.request.BoardSaveRequest;
import kwh.PublicCookedFood.board.dto.request.BoardUpdateRequest;
import kwh.PublicCookedFood.board.dto.request.BoardWriteRequest;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import kwh.PublicCookedFood.board.service.BoardService;
import kwh.PublicCookedFood.notification.service.NotificationService;
import kwh.PublicCookedFood.account.audit.BoardAuditPublisher;
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
    public Board createBoard(Long actorAccountId, BoardWriteRequest boardDto) {
        BoardSaveRequest command = BoardSaveRequest.forCreate(boardDto, actorAccountId);
        Board savedBoard = boardService.save(command);
        notificationService.notifyOnBoardCreated(savedBoard);
        boardAuditPublisher.boardCreate(actorAccountId, savedBoard.getId());
        return savedBoard;
    }

    @Transactional
    public void updateBoard(Long actorAccountId,
                            Long boardId,
                            BoardUpdateRequest boardDto,
                            BoardDetailResponse existingBoard) {
        BoardSaveRequest command = BoardSaveRequest.forUpdate(boardDto, existingBoard);
        boardService.save(command);
        if (actorAccountId != null) {
            boardAuditPublisher.boardUpdate(actorAccountId, boardId);
        }
    }

    @Transactional
    public void deleteBoard(Long actorAccountId, Long boardId) {
        boardService.delete(boardId);
        if (actorAccountId != null) {
            boardAuditPublisher.boardDelete(actorAccountId, boardId);
        }
    }
}
