package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardStats;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.board.repository.BoardStatsRepository;
import kwh.PublicCookedFood.board.repository.LikesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardStatsMutationService {

    private final BoardStatsRepository boardStatsRepository;
    private final LikesRepository likesRepository;
    private final CommentsRepository commentsRepository;

    @Transactional
    public void initializeBoard(Board board) {
        if (board == null || board.getId() == null) {
            return;
        }

        try {
            if (boardStatsRepository.existsById(board.getId())) {
                return;
            }
            boardStatsRepository.save(BoardStats.initialize(board.getId(), 0L, 0L, 0L));
        } catch (DataIntegrityViolationException e) {
            // Concurrent bootstrap or retry path inserted the row first.
            log.debug("Board stats already initialized. boardId={}", board.getId());
        } catch (RuntimeException e) {
            log.warn("Failed to initialize board stats. boardId={}", board.getId(), e);
        }
    }

    @Transactional
    public void syncLikeCount(Long boardId, long totalLikes) {
        syncCount(
                boardId,
                Math.max(0L, totalLikes),
                boardStatsRepository::updateTotalLikes,
                normalizedLikes -> initializeFromLegacyCounters(boardId, normalizedLikes, null),
                "like"
        );
    }

    @Transactional
    public void syncCommentCount(Long boardId, long totalComments) {
        syncCount(
                boardId,
                Math.max(0L, totalComments),
                boardStatsRepository::updateTotalComments,
                normalizedComments -> initializeFromLegacyCounters(boardId, null, normalizedComments),
                "comment"
        );
    }

    private BoardStats initializeFromLegacyCounters(Long boardId,
                                                    Long totalLikesOverride,
                                                    Long totalCommentsOverride) {
        long totalLikes = totalLikesOverride != null
                ? normalize(totalLikesOverride)
                : normalize(likesRepository.countByBoardId(boardId));
        long totalComments = totalCommentsOverride != null
                ? normalize(totalCommentsOverride)
                : normalize(commentsRepository.countByBoardIdAndState(boardId, SoftDeleteState.ACTIVE));
        return BoardStats.initialize(boardId, 0L, totalLikes, totalComments);
    }

    private void syncCount(Long boardId,
                           long totalCount,
                           StatsCountUpdater updater,
                           StatsFactory factory,
                           String metricName) {
        if (boardId == null) {
            return;
        }

        try {
            int updated = updater.update(boardId, totalCount);
            if (updated > 0) {
                return;
            }

            try {
                boardStatsRepository.save(factory.create(totalCount));
            } catch (DataIntegrityViolationException e) {
                updater.update(boardId, totalCount);
            }
        } catch (RuntimeException e) {
            log.warn("Failed to sync board {} stats. boardId={}, totalCount={}", metricName, boardId, totalCount, e);
        }
    }

    private long normalize(Long value) {
        return value == null ? 0L : Math.max(0L, value);
    }

    @FunctionalInterface
    private interface StatsCountUpdater {
        int update(Long boardId, Long totalCount);
    }

    @FunctionalInterface
    private interface StatsFactory {
        BoardStats create(long totalCount);
    }
}
