package kwh.PublicCookedFood.board.service;

import jakarta.transaction.Transactional;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardScrap;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.BoardScrapRepository;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.repository.UserRepository;
import kwh.PublicCookedFood.user.service.UserBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardScrapService {

    private final BoardScrapRepository boardScrapRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final UserBlockService userBlockService;

    @Transactional
    public void addScrap(Long boardId, Long userId) {
        if (boardId == null || userId == null) {
            throw new IllegalArgumentException("스크랩 요청 값이 올바르지 않습니다.");
        }
        if (boardScrapRepository.existsByBoardIdAndUserId(boardId, userId)) {
            return;
        }

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자입니다."));
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 게시글입니다."));
        if (board.getState() != SoftDeleteState.ACTIVE) {
            throw new IllegalArgumentException("삭제된 게시글은 스크랩할 수 없습니다.");
        }
        if (board.getUser() != null && userBlockService.isEitherBlocked(userId, board.getUser().getId())) {
            throw new IllegalStateException("차단 관계인 사용자의 게시글은 스크랩할 수 없습니다.");
        }

        try {
            boardScrapRepository.save(BoardScrap.builder()
                    .user(user)
                    .board(board)
                    .build());
        } catch (DataIntegrityViolationException ignored) {
            // 동시 요청 경합으로 unique 제약에 걸린 경우에는 이미 저장된 상태로 간주한다.
        }
    }

    @Transactional
    public void removeScrap(Long boardId, Long userId) {
        if (boardId == null || userId == null) {
            throw new IllegalArgumentException("스크랩 요청 값이 올바르지 않습니다.");
        }
        boardScrapRepository.deleteByBoardIdAndUserId(boardId, userId);
    }

    @Transactional
    public boolean isScrapped(Long boardId, Long userId) {
        if (boardId == null || userId == null) {
            return false;
        }
        return boardScrapRepository.existsByBoardIdAndUserId(boardId, userId);
    }

    @Transactional
    public long getScrapCount(Long boardId) {
        if (boardId == null) {
            return 0L;
        }
        return boardScrapRepository.countByBoardId(boardId);
    }

    @Transactional
    public List<Board> getMyScrappedBoards(Long userId) {
        return getMyScrappedBoards(userId, Set.of());
    }

    @Transactional
    public List<Board> getMyScrappedBoards(Long userId, Collection<Long> blockedUserIds) {
        if (userId == null) {
            return List.of();
        }
        BlockedUserFilter blockedUserFilter = resolveBlockedUserFilter(blockedUserIds);
        return boardScrapRepository.findScrappedBoardsByUserIdAndBoardState(
                userId,
                SoftDeleteState.ACTIVE,
                blockedUserFilter.excludeBlocked(),
                blockedUserFilter.blockedUserIds());
    }

    @Transactional
    public Page<Board> getScrappedBoardsPage(Long userId, Pageable pageable, Collection<Long> blockedUserIds) {
        if (userId == null) {
            return Page.empty(pageable);
        }
        BlockedUserFilter blockedUserFilter = resolveBlockedUserFilter(blockedUserIds);
        return boardScrapRepository.findScrappedBoardsPageByUserIdAndBoardState(
                userId,
                SoftDeleteState.ACTIVE,
                blockedUserFilter.excludeBlocked(),
                blockedUserFilter.blockedUserIds(),
                pageable
        );
    }

    private BlockedUserFilter resolveBlockedUserFilter(Collection<Long> blockedUserIds) {
        if (blockedUserIds == null || blockedUserIds.isEmpty()) {
            return new BlockedUserFilter(false, Set.of(-1L));
        }

        Set<Long> normalized = blockedUserIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (normalized.isEmpty()) {
            return new BlockedUserFilter(false, Set.of(-1L));
        }
        return new BlockedUserFilter(true, normalized);
    }

    private record BlockedUserFilter(boolean excludeBlocked, Collection<Long> blockedUserIds) {
    }
}
