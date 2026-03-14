package kwh.PublicCookedFood.board.service;

import org.springframework.transaction.annotation.Transactional;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Likes;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.error.BoardErrorCode;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.LikesRepository;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.CommonErrorCode;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikesRepository likesRepository;

    private final AccountRepository accountRepository;

    private final BoardRepository boardRepository;

    private final AccountBlockService accountBlockService;

    @Transactional
    public LikeSaveResult saveLikes(Long boardId, Long accountId) {
        validateBoardActor(boardId, accountId);
        if (likesRepository.existsByBoardIdAndAccountId(boardId, accountId)) {
            return LikeSaveResult.alreadyExists();
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND, "유효하지 않은 사용자입니다."));
        Board board = boardRepository.findByIdWithAccountAndState(boardId, SoftDeleteState.ACTIVE)
                .orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND, "유효하지 않은 게시글입니다."));
        if (board.getAccount() != null && accountBlockService.isEitherBlocked(accountId, board.getAccount().getId())) {
            throw new AppException(BoardErrorCode.BOARD_LIKE_BLOCKED);
        }

        Likes like = Likes.builder()
                .board(board)
                .account(account)
                .build();
        try {
            return LikeSaveResult.created(likesRepository.saveAndFlush(like).getId());
        } catch (DataIntegrityViolationException e) {
            // 동시 요청 경합으로 unique 제약 충돌 시 기존 레코드를 재조회한다.
            return likesRepository.findByBoardIdAndAccountId(boardId, accountId)
                    .<LikeSaveResult>map(existingLike -> LikeSaveResult.recovered(existingLike.getId()))
                    .orElseGet(LikeSaveResult::alreadyExists);
        }
    }

    public boolean hasLike(Long boardId, Long accountId){
        return likesRepository.existsByBoardIdAndAccountId(boardId, accountId);
    }

    private void validateBoardActor(Long boardId, Long accountId) {
        if (boardId == null || boardId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 게시글입니다.");
        }
        if (accountId == null || accountId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자입니다.");
        }
    }
}
