package kwh.PublicCookedFood.board.service.command;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.service.BoardImageService;
import kwh.PublicCookedFood.notification.service.NotificationService;
import kwh.PublicCookedFood.account.audit.BoardAuditPublisher;
import kwh.PublicCookedFood.board.service.image.BoardImageSyncCommand;
import kwh.PublicCookedFood.board.service.support.BoardContentSanitizer;
import kwh.PublicCookedFood.board.service.support.SanitizedBoardContent;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.CommonErrorCode;
import kwh.PublicCookedFood.board.service.BoardStatsMutationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BoardCommandService {

    private static final String BOARD_EDIT_CONFLICT_MESSAGE = "다른 사용자가 이미 게시글을 수정했습니다. 새로고침 후 다시 시도해주세요.";

    private final BoardRepository boardRepository;
    private final BoardImageService boardImageService;
    private final BoardSectionCommandService boardSectionCommandService;
    private final AccountRepository accountRepository;
    private final BoardContentSanitizer boardContentSanitizer;
    private final BoardStatsMutationService boardStatsMutationService;
    private final NotificationService notificationService;
    private final BoardAuditPublisher boardAuditPublisher;

    @Transactional
    public Board create(BoardCreateCommand command) {
        BoardSaveCommand saveCommand = command.saveCommand();
        SanitizedBoardContent sanitizedContent = boardContentSanitizer.sanitize(saveCommand.title(), saveCommand.contents());
        BoardSection section = boardSectionCommandService.resolveSectionForWrite(saveCommand.sectionId());
        Account account = accountRepository.findById(command.actorAccountId())
                .orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND, "유효하지 않은 사용자입니다."));
        Board board = saveCommand.toBoard(account, section, sanitizedContent.title(), sanitizedContent.contents());
        Board savedBoard = boardRepository.save(board);
        boardStatsMutationService.initializeBoard(savedBoard);
        boardImageService.syncBoardImages(BoardImageSyncCommand.of(savedBoard, sanitizedContent.contents()));
        notificationService.notifyOnBoardCreated(savedBoard);
        boardAuditPublisher.boardCreate(command.actorAccountId(), savedBoard.getId());
        return savedBoard;
    }

    @Transactional
    public void update(BoardUpdateCommand command) {
        SanitizedBoardContent sanitizedContent = boardContentSanitizer.sanitize(command.title(), command.contents());
        BoardSection section = boardSectionCommandService.resolveSectionForWrite(command.sectionId());
        Board board = boardRepository.findByIdWithAccountAndState(command.boardId(), SoftDeleteState.ACTIVE)
                .orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND, "유효하지 않은 게시글입니다."));
        if (!Objects.equals(board.getVersion(), command.version())) {
            throw new AppException(CommonErrorCode.REQUEST_CONFLICT, BOARD_EDIT_CONFLICT_MESSAGE);
        }
        board.edit(sanitizedContent.title(), sanitizedContent.contents(), section);
        try {
            boardRepository.flush();
        } catch (OptimisticLockingFailureException e) {
            throw new AppException(CommonErrorCode.REQUEST_CONFLICT, BOARD_EDIT_CONFLICT_MESSAGE, e);
        }
        boardImageService.syncBoardImages(BoardImageSyncCommand.of(board, sanitizedContent.contents()));
        boardAuditPublisher.boardUpdate(command.actorAccountId(), command.boardId());
    }

    @Transactional
    public void delete(long boardId, long actorAccountId) {
        if (boardId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 게시글입니다.");
        }
        if (actorAccountId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자입니다.");
        }
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND, "유효하지 않은 게시글입니다."));
        boardImageService.deleteBoardImages(board);
        boardRepository.updateState(boardId, SoftDeleteState.DELETED);
        boardAuditPublisher.boardDelete(actorAccountId, boardId);
    }
}
