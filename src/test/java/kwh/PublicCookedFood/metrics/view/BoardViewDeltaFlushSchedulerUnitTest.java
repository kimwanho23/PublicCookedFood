package kwh.PublicCookedFood.metrics.view;

import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardRepository;
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

import java.util.Optional;

import static org.mockito.Mockito.never;
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
        when(boardRepository.addViews(7L, 5L, SoftDeleteState.ACTIVE)).thenReturn(1);
        when(boardRepository.findViewsByIdAndState(7L, SoftDeleteState.ACTIVE)).thenReturn(Optional.of(15L));
        when(boardRepository.existsByIdAndStateAndHiddenByReportFalse(7L, SoftDeleteState.ACTIVE)).thenReturn(true);
        when(boardStatsRepository.updateTotalViews(7L, 15L)).thenReturn(1);

        scheduler.flushBoardViewDeltas();

        verify(boardRepository).addViews(7L, 5L, SoftDeleteState.ACTIVE);
        verify(boardStatsRepository).updateTotalViews(7L, 15L);
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
        when(boardRepository.addViews(8L, 3L, SoftDeleteState.ACTIVE)).thenThrow(new RuntimeException("db fail"));

        scheduler.flushBoardViewDeltas();

        verify(valueOperations).increment(ViewCounterRedisKeys.boardDeltaKey(8L), 3L);
        verify(setOperations).add(ViewCounterRedisKeys.BOARD_DIRTY_SET_KEY, "8");
    }
}
