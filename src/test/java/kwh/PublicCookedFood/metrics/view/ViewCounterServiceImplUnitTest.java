package kwh.PublicCookedFood.metrics.view;

import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewCounterServiceImplUnitTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardStatsRepository boardStatsRepository;

    @Mock
    private ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private ViewCounterServiceImpl viewCounterService;

    @Test
    void increaseBoardViewAndGet_usesDbWhenRedisDisabled() {
        ReflectionTestUtils.setField(viewCounterService, "redisEnabled", false);
        when(boardRepository.updateViews(1L, SoftDeleteState.ACTIVE)).thenReturn(1);
        when(boardRepository.findViewsByIdAndState(1L, SoftDeleteState.ACTIVE))
                .thenReturn(Optional.of(11L), Optional.of(11L));
        when(boardRepository.existsByIdAndStateAndHiddenByReportFalse(1L, SoftDeleteState.ACTIVE)).thenReturn(true);
        when(boardStatsRepository.updateTotalViews(1L, 11L)).thenReturn(1);

        long result = viewCounterService.increaseBoardViewAndGet(1L);

        assertThat(result).isEqualTo(11L);
        verify(boardRepository).updateViews(1L, SoftDeleteState.ACTIVE);
        verify(redisTemplateProvider, never()).getIfAvailable();
    }

    @Test
    void increaseBoardViewAndGet_usesRedisWhenEnabled() {
        ReflectionTestUtils.setField(viewCounterService, "redisEnabled", true);
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(boardStatsRepository.findTotalViewsByBoardId(2L)).thenReturn(Optional.of(20L));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(valueOperations.get(ViewCounterRedisKeys.boardTotalKey(2L))).thenReturn(null);
        when(valueOperations.increment(ViewCounterRedisKeys.boardTotalKey(2L))).thenReturn(21L);
        when(valueOperations.increment(ViewCounterRedisKeys.boardDeltaKey(2L))).thenReturn(1L);

        long result = viewCounterService.increaseBoardViewAndGet(2L);

        assertThat(result).isEqualTo(21L);
        verify(boardRepository, never()).updateViews(2L, SoftDeleteState.ACTIVE);
        verify(valueOperations).setIfAbsent(ViewCounterRedisKeys.boardTotalKey(2L), "20");
        verify(setOperations).add(ViewCounterRedisKeys.BOARD_DIRTY_SET_KEY, "2");
    }

    @Test
    void increaseBoardViewAndGet_skipsDbReadWhenTotalKeyAlreadyExists() {
        ReflectionTestUtils.setField(viewCounterService, "redisEnabled", true);
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(valueOperations.get(ViewCounterRedisKeys.boardTotalKey(4L))).thenReturn("100");
        when(valueOperations.increment(ViewCounterRedisKeys.boardTotalKey(4L))).thenReturn(101L);
        when(valueOperations.increment(ViewCounterRedisKeys.boardDeltaKey(4L))).thenReturn(1L);

        long result = viewCounterService.increaseBoardViewAndGet(4L);

        assertThat(result).isEqualTo(101L);
        verify(boardRepository, never()).findViewsByIdAndState(4L, SoftDeleteState.ACTIVE);
        verify(valueOperations, never()).setIfAbsent(ViewCounterRedisKeys.boardTotalKey(4L), "100");
    }

    @Test
    void increaseBoardViewAndGet_fallsBackToDbOnRedisFailure() {
        ReflectionTestUtils.setField(viewCounterService, "redisEnabled", true);
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(boardRepository.updateViews(3L, SoftDeleteState.ACTIVE)).thenReturn(1);
        when(boardRepository.findViewsByIdAndState(3L, SoftDeleteState.ACTIVE))
                .thenReturn(Optional.of(31L), Optional.of(31L));
        when(boardRepository.existsByIdAndStateAndHiddenByReportFalse(3L, SoftDeleteState.ACTIVE)).thenReturn(true);
        when(boardStatsRepository.updateTotalViews(3L, 31L)).thenReturn(1);
        when(redisTemplate.opsForValue()).thenThrow(new DataAccessResourceFailureException("redis down"));

        long result = viewCounterService.increaseBoardViewAndGet(3L);

        assertThat(result).isEqualTo(31L);
        verify(boardRepository).updateViews(3L, SoftDeleteState.ACTIVE);
    }

    @Test
    void getBoardViewCount_prefersRedisValueWhenAvailable() {
        ReflectionTestUtils.setField(viewCounterService, "redisEnabled", true);
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(List.of(ViewCounterRedisKeys.boardTotalKey(9L))))
                .thenReturn(List.of("77"));

        long result = viewCounterService.getBoardViewCount(9L);

        assertThat(result).isEqualTo(77L);
        verify(boardStatsRepository, never()).findTotalViewsByBoardId(9L);
    }

    @Test
    void getBoardViewCounts_usesStatsThenBoardFallback() {
        ReflectionTestUtils.setField(viewCounterService, "redisEnabled", false);
        when(boardStatsRepository.findViewCountsByBoardIdIn(Set.of(1L, 2L)))
                .thenReturn(List.of(statsRow(1L, 10L)));
        when(boardRepository.findViewsByIdInAndState(Set.of(2L), SoftDeleteState.ACTIVE))
                .thenReturn(List.of(boardRow(2L, 20L)));

        Map<Long, Long> result = viewCounterService.getBoardViewCounts(List.of(1L, 2L));

        assertThat(result).containsEntry(1L, 10L);
        assertThat(result).containsEntry(2L, 20L);
    }

    @Test
    void getBoardViewCounts_fallsBackToBoardWhenStatsQueryFails() {
        ReflectionTestUtils.setField(viewCounterService, "redisEnabled", false);
        when(boardStatsRepository.findViewCountsByBoardIdIn(Set.of(7L)))
                .thenThrow(new DataAccessResourceFailureException("board_stats missing"));
        when(boardRepository.findViewsByIdInAndState(Set.of(7L), SoftDeleteState.ACTIVE))
                .thenReturn(List.of(boardRow(7L, 70L)));

        Map<Long, Long> result = viewCounterService.getBoardViewCounts(List.of(7L));

        assertThat(result).containsEntry(7L, 70L);
    }

    @Test
    void getBoardViewCount_fallsBackToBoardWhenStatsQueryFails() {
        ReflectionTestUtils.setField(viewCounterService, "redisEnabled", false);
        when(boardStatsRepository.findTotalViewsByBoardId(8L))
                .thenThrow(new DataAccessResourceFailureException("board_stats missing"));
        when(boardRepository.findViewsByIdAndState(8L, SoftDeleteState.ACTIVE))
                .thenReturn(Optional.of(81L));

        long result = viewCounterService.getBoardViewCount(8L);

        assertThat(result).isEqualTo(81L);
    }

    @Test
    void getBoardViewCount_propagatesWhenBoardFallbackQueryFails() {
        ReflectionTestUtils.setField(viewCounterService, "redisEnabled", false);
        when(boardStatsRepository.findTotalViewsByBoardId(12L))
                .thenThrow(new DataAccessResourceFailureException("board_stats missing"));
        when(boardRepository.findViewsByIdAndState(12L, SoftDeleteState.ACTIVE))
                .thenThrow(new DataAccessResourceFailureException("board table unavailable"));

        assertThatThrownBy(() -> viewCounterService.getBoardViewCount(12L))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessageContaining("board table unavailable");
    }

    @Test
    void getBoardViewCounts_propagatesWhenBoardFallbackQueryFails() {
        ReflectionTestUtils.setField(viewCounterService, "redisEnabled", false);
        when(boardStatsRepository.findViewCountsByBoardIdIn(Set.of(13L)))
                .thenThrow(new DataAccessResourceFailureException("board_stats missing"));
        when(boardRepository.findViewsByIdInAndState(Set.of(13L), SoftDeleteState.ACTIVE))
                .thenThrow(new DataAccessResourceFailureException("board table unavailable"));

        assertThatThrownBy(() -> viewCounterService.getBoardViewCounts(List.of(13L)))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessageContaining("board table unavailable");
    }

    private BoardStatsRepository.BoardViewCountProjection statsRow(Long boardId, Long totalViews) {
        return new BoardStatsRepository.BoardViewCountProjection() {
            @Override
            public Long getBoardId() {
                return boardId;
            }

            @Override
            public Long getTotalViews() {
                return totalViews;
            }
        };
    }

    private BoardRepository.BoardViewCountProjection boardRow(Long boardId, Long views) {
        return new BoardRepository.BoardViewCountProjection() {
            @Override
            public Long getBoardId() {
                return boardId;
            }

            @Override
            public Long getViews() {
                return views;
            }
        };
    }
}
