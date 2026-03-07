package kwh.PublicCookedFood.metrics.view;

import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViewCounterServiceImpl implements ViewCounterService {

    private final BoardRepository boardRepository;
    private final BoardStatsRepository boardStatsRepository;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    @Value("${app.view-counter.redis.enabled:false}")
    private boolean redisEnabled;

    @Override
    @Transactional(readOnly = true)
    public long getBoardViewCount(Long boardId) {
        if (boardId == null) {
            throw new IllegalArgumentException("게시글 ID는 필수입니다.");
        }
        return readCurrentBoardViews(boardId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getBoardViewCounts(Collection<Long> boardIds) {
        Set<Long> normalizedIds = normalizeBoardIds(boardIds);
        if (normalizedIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> counts = new LinkedHashMap<>();
        Map<Long, Long> redisCounts = readViewsFromRedis(normalizedIds);
        counts.putAll(redisCounts);

        Set<Long> remainingIds = new LinkedHashSet<>(normalizedIds);
        remainingIds.removeAll(redisCounts.keySet());
        mergeStatsCounts(counts, remainingIds);
        mergeBoardCounts(counts, remainingIds);
        populateMissingCounts(normalizedIds, counts);
        return counts;
    }

    @Override
    @Transactional
    public long increaseBoardViewAndGet(Long boardId) {
        if (boardId == null) {
            throw new IllegalArgumentException("게시글 ID는 필수입니다.");
        }
        if (!redisEnabled) {
            return increaseWithDb(boardId);
        }

        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return increaseWithDb(boardId);
        }

        try {
            return increaseWithRedis(redisTemplate, boardId);
        } catch (DataAccessException e) {
            log.warn("Redis view counter failed. Fallback to DB increment. boardId={}", boardId, e);
            return increaseWithDb(boardId);
        }
    }

    private long increaseWithDb(Long boardId) {
        int updatedRows = boardRepository.updateViews(boardId, SoftDeleteState.ACTIVE);
        if (updatedRows <= 0) {
            return readCurrentBoardViews(boardId);
        }
        long boardViews = boardRepository.findViewsByIdAndState(boardId, SoftDeleteState.ACTIVE).orElse(0L);
        syncStatsFromBoardViews(boardId);
        return boardViews;
    }

    private long readCurrentBoardViews(Long boardId) {
        Long redisViews = readViewsFromRedis(boardId);
        if (redisViews != null) {
            return redisViews;
        }

        Long statsViews = readViewsFromStats(boardId);
        if (statsViews != null) {
            return statsViews;
        }

        return readViewsFromBoardTable(boardId);
    }

    private void syncStatsFromBoardViews(Long boardId) {
        Long boardViews = boardRepository.findViewsByIdAndState(boardId, SoftDeleteState.ACTIVE).orElse(null);
        if (boardViews == null) {
            return;
        }
        try {
            upsertBoardStats(boardId, boardViews);
        } catch (DataAccessException e) {
            // Keep request success path intact even when stats mirror update fails.
            log.warn("Failed to sync board_stats from board views. boardId={}", boardId, e);
        }
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

    private Long readViewsFromRedis(Long boardId) {
        return readViewsFromRedis(List.of(boardId)).get(boardId);
    }

    private Map<Long, Long> readViewsFromRedis(Collection<Long> boardIds) {
        if (!redisEnabled) {
            return Map.of();
        }

        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return Map.of();
        }

        try {
            List<Long> orderedIds = new ArrayList<>(boardIds);
            List<String> keys = buildRedisKeys(orderedIds);
            List<String> values = redisTemplate.opsForValue().multiGet(keys);
            if (values == null || values.isEmpty()) {
                return Map.of();
            }

            return parseRedisValues(orderedIds, values);
        } catch (DataAccessException e) {
            log.warn("Failed to read board views from Redis batch. size={}", boardIds.size(), e);
            return Map.of();
        }
    }

    private Set<Long> normalizeBoardIds(Collection<Long> boardIds) {
        if (boardIds == null || boardIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> normalizedIds = new LinkedHashSet<>();
        for (Long boardId : boardIds) {
            if (boardId != null) {
                normalizedIds.add(boardId);
            }
        }
        return normalizedIds;
    }

    private void mergeStatsCounts(Map<Long, Long> counts, Set<Long> remainingIds) {
        if (remainingIds.isEmpty()) {
            return;
        }
        try {
            mergeStatsRows(counts, boardStatsRepository.findViewCountsByBoardIdIn(remainingIds));
        } catch (DataAccessException e) {
            log.warn("Failed to read board_stats view counts. size={}", remainingIds.size(), e);
        }
        remainingIds.removeAll(counts.keySet());
    }

    private void mergeStatsRows(Map<Long, Long> counts,
                                List<BoardStatsRepository.BoardViewCountProjection> statsRows) {
        if (statsRows == null) {
            return;
        }
        for (BoardStatsRepository.BoardViewCountProjection row : statsRows) {
            if (row.getBoardId() == null || row.getTotalViews() == null) {
                continue;
            }
            counts.put(row.getBoardId(), row.getTotalViews());
        }
    }

    private void mergeBoardCounts(Map<Long, Long> counts, Set<Long> remainingIds) {
        if (remainingIds.isEmpty()) {
            return;
        }
        try {
            mergeBoardRows(counts, boardRepository.findViewsByIdInAndState(remainingIds, SoftDeleteState.ACTIVE));
        } catch (DataAccessException e) {
            log.error("Failed to read board view counts from board table. size={}", remainingIds.size(), e);
            throw e;
        }
    }

    private void mergeBoardRows(Map<Long, Long> counts,
                                List<BoardRepository.BoardViewCountProjection> boardRows) {
        if (boardRows == null) {
            return;
        }
        for (BoardRepository.BoardViewCountProjection row : boardRows) {
            if (row.getBoardId() == null || row.getViews() == null) {
                continue;
            }
            counts.put(row.getBoardId(), row.getViews());
        }
    }

    private void populateMissingCounts(Set<Long> normalizedIds, Map<Long, Long> counts) {
        for (Long boardId : normalizedIds) {
            counts.putIfAbsent(boardId, 0L);
        }
    }

    private long increaseWithRedis(StringRedisTemplate redisTemplate, Long boardId) {
        initializeRedisTotalIfMissing(redisTemplate, boardId);
        Long increased = incrementRedisTotal(redisTemplate, boardId);
        markDirty(redisTemplate, boardId);
        if (increased != null) {
            return increased;
        }
        return increaseWithDb(boardId);
    }

    private void initializeRedisTotalIfMissing(StringRedisTemplate redisTemplate, Long boardId) {
        String totalKey = ViewCounterRedisKeys.boardTotalKey(boardId);
        String cachedTotal = redisTemplate.opsForValue().get(totalKey);
        if (cachedTotal != null) {
            return;
        }
        long baseViews = readCurrentBoardViews(boardId);
        redisTemplate.opsForValue().setIfAbsent(totalKey, Long.toString(baseViews));
    }

    private Long incrementRedisTotal(StringRedisTemplate redisTemplate, Long boardId) {
        Long increased = redisTemplate.opsForValue().increment(ViewCounterRedisKeys.boardTotalKey(boardId));
        redisTemplate.opsForValue().increment(ViewCounterRedisKeys.boardDeltaKey(boardId));
        return increased;
    }

    private void markDirty(StringRedisTemplate redisTemplate, Long boardId) {
        redisTemplate.opsForSet().add(ViewCounterRedisKeys.BOARD_DIRTY_SET_KEY, boardId.toString());
    }

    private Long readViewsFromStats(Long boardId) {
        try {
            return boardStatsRepository.findTotalViewsByBoardId(boardId).orElse(null);
        } catch (DataAccessException e) {
            log.warn("Failed to read board_stats total views. boardId={}", boardId, e);
            return null;
        }
    }

    private long readViewsFromBoardTable(Long boardId) {
        try {
            return boardRepository.findViewsByIdAndState(boardId, SoftDeleteState.ACTIVE)
                    .orElse(0L);
        } catch (DataAccessException e) {
            log.error("Failed to read board views from board table. boardId={}", boardId, e);
            throw e;
        }
    }

    private List<String> buildRedisKeys(List<Long> orderedIds) {
        return orderedIds.stream()
                .map(ViewCounterRedisKeys::boardTotalKey)
                .toList();
    }

    private Map<Long, Long> parseRedisValues(List<Long> orderedIds, List<String> values) {
        Map<Long, Long> parsed = new LinkedHashMap<>();
        for (int i = 0; i < orderedIds.size() && i < values.size(); i++) {
            String raw = values.get(i);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            Long boardId = orderedIds.get(i);
            if (boardId == null) {
                continue;
            }
            try {
                parsed.put(boardId, Long.parseLong(raw));
            } catch (NumberFormatException ignored) {
                // Skip malformed value and fall back to stats/board for this boardId.
            }
        }
        return parsed;
    }
}
