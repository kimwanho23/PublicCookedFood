package kwh.PublicCookedFood.metrics.view;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardStats;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.BoardStatsRepository;
import kwh.PublicCookedFood.board.service.BoardViewDeltaFlushScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardViewDeltaFlushSchedulerUnitTest {

    @Mock
    private ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardStatsRepository boardStatsRepository;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private BoardViewDeltaFlushScheduler scheduler;

    @Test
    void flushBoardViewDeltas_returnsImmediatelyWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "redisEnabled", false);
        ReflectionTestUtils.setField(scheduler, "flushEnabled", true);

        scheduler.flushBoardViewDeltas();

        verify(redisTemplateProvider, never()).getIfAvailable();
    }

    @Test
    void flushBoardViewDeltas_appliesDeltaToDatabase() {
        ReflectionTestUtils.setField(scheduler, "redisEnabled", true);
        ReflectionTestUtils.setField(scheduler, "flushEnabled", true);
        ReflectionTestUtils.setField(scheduler, "flushBatchSize", 10);
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(setOperations.pop(ViewCounterRedisKeys.BOARD_DIRTY_SET_KEY)).thenReturn("7").thenReturn(null);
        when(valueOperations.getAndSet(ViewCounterRedisKeys.boardDeltaKey(7L), "0")).thenReturn("5");
        when(boardRepository.existsByIdAndStateAndHiddenByReportFalse(7L, SoftDeleteState.ACTIVE)).thenReturn(true);
        when(boardStatsRepository.addViews(7L, 5L)).thenReturn(1);

        scheduler.flushBoardViewDeltas();

        verify(boardStatsRepository).addViews(7L, 5L);
    }

    @Test
    void flushBoardViewDeltas_restoresDeltaWhenDatabaseFlushFails() {
        ReflectionTestUtils.setField(scheduler, "redisEnabled", true);
        ReflectionTestUtils.setField(scheduler, "flushEnabled", true);
        ReflectionTestUtils.setField(scheduler, "flushBatchSize", 10);
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(setOperations.pop(ViewCounterRedisKeys.BOARD_DIRTY_SET_KEY)).thenReturn("8").thenReturn(null);
        when(valueOperations.getAndSet(ViewCounterRedisKeys.boardDeltaKey(8L), "0")).thenReturn("3");
        when(boardRepository.existsByIdAndStateAndHiddenByReportFalse(8L, SoftDeleteState.ACTIVE)).thenReturn(true);
        when(boardStatsRepository.addViews(8L, 3L)).thenThrow(new RuntimeException("db fail"));

        scheduler.flushBoardViewDeltas();

        verify(valueOperations).increment(ViewCounterRedisKeys.boardDeltaKey(8L), 3L);
        verify(setOperations).add(ViewCounterRedisKeys.BOARD_DIRTY_SET_KEY, "8");
    }

    @Test
    void flushBoardViewDeltas_initializesStatsFromLockedBoardWhenMissing() {
        ReflectionTestUtils.setField(scheduler, "redisEnabled", true);
        ReflectionTestUtils.setField(scheduler, "flushEnabled", true);
        ReflectionTestUtils.setField(scheduler, "flushBatchSize", 10);
        Board board = Board.builder()
                .id(9L)
                .state(SoftDeleteState.ACTIVE)
                .hiddenByReport(false)
                .build();
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(setOperations.pop(ViewCounterRedisKeys.BOARD_DIRTY_SET_KEY)).thenReturn("9").thenReturn(null);
        when(valueOperations.getAndSet(ViewCounterRedisKeys.boardDeltaKey(9L), "0")).thenReturn("4");
        when(boardRepository.existsByIdAndStateAndHiddenByReportFalse(9L, SoftDeleteState.ACTIVE)).thenReturn(true);
        when(boardStatsRepository.addViews(9L, 4L)).thenReturn(0, 0, 1);
        when(boardRepository.findByIdAndStateAndHiddenByReportFalseForUpdate(9L, SoftDeleteState.ACTIVE))
                .thenReturn(java.util.Optional.of(board));

        scheduler.flushBoardViewDeltas();

        verify(boardStatsRepository).save(org.mockito.ArgumentMatchers.any(BoardStats.class));
        verify(boardStatsRepository, times(3)).addViews(9L, 4L);
    }
}
