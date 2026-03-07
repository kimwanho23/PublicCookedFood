package kwh.PublicCookedFood.metrics.view;

import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
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
                int updated = boardRepository.addViews(boardId, delta, SoftDeleteState.ACTIVE);
                if (updated <= 0) {
                    log.debug("Skip delta flush for inactive/hidden board. boardId={}, delta={}", boardId, delta);
                    continue;
                }
                try {
                    syncBoardStatsFromBoardViews(boardId);
                } catch (RuntimeException e) {
                    // board table increment already succeeded; keep going and recover stats via next sync.
                    log.warn("Failed to sync board_stats after delta flush. boardId={}, delta={}", boardId, delta, e);
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

    private void syncBoardStatsFromBoardViews(Long boardId) {
        Long boardViews = boardRepository.findViewsByIdAndState(boardId, SoftDeleteState.ACTIVE).orElse(null);
        if (boardViews == null) {
            return;
        }
        upsertBoardStats(boardId, boardViews);
    }

    private void upsertBoardStats(Long boardId, long totalViews) {
        if (!boardRepository.existsByIdAndStateAndHiddenByReportFalse(boardId, SoftDeleteState.ACTIVE)) {
            return;
        }

        int updatedRows = boardStatsRepository.updateTotalViews(boardId, totalViews);
        if (updatedRows > 0) {
            return;
        }

        try {
            boardStatsRepository.save(BoardStats.initialize(boardId, totalViews));
        } catch (DataIntegrityViolationException e) {
            boardStatsRepository.updateTotalViews(boardId, totalViews);
        }
    }
}
