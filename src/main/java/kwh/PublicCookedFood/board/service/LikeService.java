package kwh.PublicCookedFood.board.service;

import org.springframework.transaction.annotation.Transactional;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Likes;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.error.BoardErrorCode;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.LikesRepository;
import kwh.PublicCookedFood.common.error.AppException;
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
    public Long saveLikes(Long boardId, Long accountId) {
        if (likesRepository.existsByBoardIdAndAccountId(boardId, accountId)) {
            return null;
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid account ID"));
        Board board = boardRepository.findByIdWithAccountAndState(boardId, SoftDeleteState.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 게시글입니다."));
        if (board.getAccount() != null && accountBlockService.isEitherBlocked(accountId, board.getAccount().getId())) {
            throw new AppException(BoardErrorCode.BOARD_LIKE_BLOCKED);
        }

        Likes like = Likes.builder()
                .board(board)
                .account(account)
                .build();
        try {
            return likesRepository.saveAndFlush(like).getId();
        } catch (DataIntegrityViolationException e) {
            // 동시 요청 경합으로 unique 제약 충돌 시 기존 레코드를 재조회한다.
            return likesRepository.findByBoardIdAndAccountId(boardId, accountId)
                    .map(Likes::getId)
                    .orElse(null);
        }
    }

    public Long getLike(Long id){
        return likesRepository.countByBoardId(id);
    }

    public Boolean findMyLike(Long boardId, Long accountId){
        return likesRepository.existsByBoardIdAndAccountId(boardId, accountId);
    }
}
