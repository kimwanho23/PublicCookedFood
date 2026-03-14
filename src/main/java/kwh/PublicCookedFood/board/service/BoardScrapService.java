package kwh.PublicCookedFood.board.service;

import org.springframework.transaction.annotation.Transactional;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardScrap;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.error.BoardErrorCode;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.BoardScrapRepository;
import kwh.PublicCookedFood.board.service.support.BoardVisibilityCriteria;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.CommonErrorCode;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardScrapService {

    private final BoardScrapRepository boardScrapRepository;
    private final BoardRepository boardRepository;
    private final AccountRepository accountRepository;
    private final AccountBlockService accountBlockService;

    @Transactional
    public void addScrap(Long boardId, Long accountId) {
        validateBoardActor(boardId, accountId);
        if (boardScrapRepository.existsByBoardIdAndAccountId(boardId, accountId)) {
            return;
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND, "유효하지 않은 사용자입니다."));
        Board board = boardRepository.findByIdWithAccountAndState(boardId, SoftDeleteState.ACTIVE)
                .orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND, "유효하지 않은 게시글입니다."));
        if (board.getAccount() != null && accountBlockService.isEitherBlocked(accountId, board.getAccount().getId())) {
            throw new AppException(BoardErrorCode.BOARD_SCRAP_BLOCKED);
        }

        try {
            boardScrapRepository.saveAndFlush(BoardScrap.builder()
                    .account(account)
                    .board(board)
                    .build());
        } catch (DataIntegrityViolationException ignored) {
            // 동시 요청 경합으로 unique 제약에 걸린 경우에는 이미 저장된 상태로 간주한다.
        }
    }

    @Transactional
    public void removeScrap(Long boardId, Long accountId) {
        validateBoardActor(boardId, accountId);
        boardScrapRepository.deleteByBoardIdAndAccountId(boardId, accountId);
    }

    @Transactional(readOnly = true)
    public boolean isScrapped(Long boardId, Long accountId) {
        if (boardId == null || accountId == null) {
            return false;
        }
        return boardScrapRepository.existsByBoardIdAndAccountId(boardId, accountId);
    }

    @Transactional(readOnly = true)
    public long getScrapCount(Long boardId) {
        if (boardId == null) {
            return 0L;
        }
        return boardScrapRepository.countByBoardId(boardId);
    }

    @Transactional(readOnly = true)
    public List<Board> getMyScrappedBoards(Long accountId, Collection<Long> blockedAccountIds) {
        if (accountId == null) {
            return List.of();
        }
        BoardVisibilityCriteria visibility = BoardVisibilityCriteria.of(blockedAccountIds);
        return boardScrapRepository.findScrappedBoardsByAccountIdAndBoardState(
                accountId,
                SoftDeleteState.ACTIVE,
                visibility.excludeRestricted(),
                visibility.restrictedAccountIdsOrSentinel());
    }

    @Transactional(readOnly = true)
    public Page<Board> getScrappedBoardsPage(Long accountId, Pageable pageable, Collection<Long> blockedAccountIds) {
        if (accountId == null) {
            return Page.empty(pageable);
        }
        BoardVisibilityCriteria visibility = BoardVisibilityCriteria.of(blockedAccountIds);
        return boardScrapRepository.findScrappedBoardsPageByAccountIdAndBoardState(
                accountId,
                SoftDeleteState.ACTIVE,
                visibility.excludeRestricted(),
                visibility.restrictedAccountIdsOrSentinel(),
                pageable
        );
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
