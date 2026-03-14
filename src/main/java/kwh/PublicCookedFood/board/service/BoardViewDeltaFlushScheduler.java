package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.board.domain.BoardStats;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.BoardStatsRepository;
import kwh.PublicCookedFood.metrics.view.ViewCounterRedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BoardViewDeltaFlushScheduler {

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final BoardRepository boardRepository;
    private final BoardStatsRepository boardStatsRepository;

    @Value("${app.view-counter.redis.enabled:false}")
    private boolean redisEnabled;

    @Value("${app.view-counter.redis.flush-enabled:false}")
    private boolean flushEnabled;

    @Value("${app.view-counter.redis.flush-batch-size:200}")
    private int flushBatchSize;

    @Scheduled(fixedDelayString = "${app.view-counter.redis.flush-interval-ms:2000}")
    public void flushBoardViewDeltas() {
        if (!redisEnabled || !flushEnabled) {
            return;
        }

        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }

        int normalizedBatchSize = Math.max(1, flushBatchSize);
        long totalAppliedDelta = 0L;
        int flushedBoards = 0;

        for (int i = 0; i < normalizedBatchSize; i++) {
            String boardIdRaw = redisTemplate.opsForSet().pop(ViewCounterRedisKeys.BOARD_DIRTY_SET_KEY);
            if (boardIdRaw == null) {
                break;
            }

            Long boardId = parseBoardId(boardIdRaw);
            if (boardId == null) {
                continue;
            }

            String deltaKey = ViewCounterRedisKeys.boardDeltaKey(boardId);
            long delta = readAndResetDelta(redisTemplate, deltaKey);
            if (delta <= 0) {
                continue;
            }

            try {
                boolean applied = applyDeltaToBoardStats(boardId, delta);
                if (!applied) {
                    log.debug("Skip delta flush for inactive/hidden board. boardId={}, delta={}", boardId, delta);
                    continue;
                }
                flushedBoards++;
                totalAppliedDelta += delta;
            } catch (RuntimeException e) {
                restoreDelta(redisTemplate, boardIdRaw, deltaKey, delta);
                log.warn("Failed to flush board views delta. boardId={}, delta={}", boardId, delta, e);
            }
        }

        if (flushedBoards > 0) {
            log.debug("Flushed board view deltas. boards={}, totalDelta={}", flushedBoards, totalAppliedDelta);
        }
    }

    private boolean applyDeltaToBoardStats(Long boardId, long delta) {
        if (!boardRepository.existsByIdAndStateAndHiddenByReportFalse(boardId, SoftDeleteState.ACTIVE)) {
            return false;
        }

        int updated = boardStatsRepository.addViews(boardId, delta);
        if (updated > 0) {
            return true;
        }

        if (boardRepository.findByIdAndStateAndHiddenByReportFalseForUpdate(boardId, SoftDeleteState.ACTIVE).isEmpty()) {
            return false;
        }

        if (boardStatsRepository.addViews(boardId, delta) > 0) {
            return true;
        }

        try {
            boardStatsRepository.save(BoardStats.initialize(boardId, 0L));
        } catch (DataIntegrityViolationException e) {
            return boardStatsRepository.addViews(boardId, delta) > 0;
        }
        return boardStatsRepository.addViews(boardId, delta) > 0;
    }

    private Long parseBoardId(String boardIdRaw) {
        try {
            return Long.parseLong(boardIdRaw);
        } catch (NumberFormatException e) {
            log.warn("Invalid board id in dirty set. raw={}", boardIdRaw);
            return null;
        }
    }

    private long readAndResetDelta(StringRedisTemplate redisTemplate, String deltaKey) {
        String rawDelta = redisTemplate.opsForValue().getAndSet(deltaKey, "0");
        if (rawDelta == null || rawDelta.isBlank()) {
            return 0L;
        }

        try {
            return Long.parseLong(rawDelta);
        } catch (NumberFormatException e) {
            log.warn("Invalid board delta value. key={}, value={}", deltaKey, rawDelta);
            return 0L;
        }
    }

    private void restoreDelta(StringRedisTemplate redisTemplate,
                              String boardIdRaw,
                              String deltaKey,
                              long delta) {
        redisTemplate.opsForValue().increment(deltaKey, delta);
        redisTemplate.opsForSet().add(ViewCounterRedisKeys.BOARD_DIRTY_SET_KEY, boardIdRaw);
    }

}
