package kwh.PublicCookedFood.metrics.view;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardStats;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.BoardStatsRepository;
import kwh.PublicCookedFood.board.service.BoardViewCounterServiceImpl;
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
    private BoardViewCounterServiceImpl viewCounterService;

    @Test
    void increaseBoardViewAndGet_usesDbWhenRedisDisabled() {
        ReflectionTestUtils.setField(viewCounterService, "redisEnabled", false);
        when(boardRepository.existsByIdAndStateAndHiddenByReportFalse(1L, SoftDeleteState.ACTIVE)).thenReturn(true);
        when(boardStatsRepository.addViews(1L, 1L)).thenReturn(1);
        when(boardStatsRepository.findTotalViewsByBoardId(1L)).thenReturn(Optional.of(11L));

        long result = viewCounterService.increaseBoardViewAndGet(1L);

        assertThat(result).isEqualTo(11L);
        verify(boardStatsRepository).addViews(1L, 1L);
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
        verify(valueOperations, never()).setIfAbsent(ViewCounterRedisKeys.boardTotalKey(4L), "100");
    }

    @Test
    void increaseBoardViewAndGet_fallsBackToDbOnRedisFailure() {
        ReflectionTestUtils.setField(viewCounterService, "redisEnabled", true);
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(boardRepository.existsByIdAndStateAndHiddenByReportFalse(3L, SoftDeleteState.ACTIVE)).thenReturn(true);
        when(boardStatsRepository.addViews(3L, 1L)).thenReturn(1);
        when(boardStatsRepository.findTotalViewsByBoardId(3L)).thenReturn(Optional.of(31L));
        when(redisTemplate.opsForValue()).thenThrow(new DataAccessResourceFailureException("redis down"));

        long result = viewCounterService.increaseBoardViewAndGet(3L);

        assertThat(result).isEqualTo(31L);
        verify(boardStatsRepository).addViews(3L, 1L);
    }

    @Test
    void increaseBoardViewAndGet_returnsCurrentStatsWhenStatsWriteFails() {
        ReflectionTestUtils.setField(viewCounterService, "redisEnabled", false);
        when(boardRepository.existsByIdAndStateAndHiddenByReportFalse(14L, SoftDeleteState.ACTIVE)).thenReturn(true);
        when(boardStatsRepository.addViews(14L, 1L))
                .thenThrow(new DataAccessResourceFailureException("board_stats unavailable"));
        when(boardStatsRepository.findTotalViewsByBoardId(14L)).thenReturn(Optional.of(41L));

        long result = viewCounterService.increaseBoardViewAndGet(14L);

        assertThat(result).isEqualTo(41L);
    }

    @Test
    void increaseBoardViewAndGet_initializesStatsFromLockedBoardWhenMissing() {
        ReflectionTestUtils.setField(viewCounterService, "redisEnabled", false);
        Board board = Board.builder()
                .id(15L)
                .state(SoftDeleteState.ACTIVE)
                .hiddenByReport(false)
                .build();
        when(boardRepository.existsByIdAndStateAndHiddenByReportFalse(15L, SoftDeleteState.ACTIVE)).thenReturn(true);
        when(boardStatsRepository.addViews(15L, 1L)).thenReturn(0, 0, 1);
        when(boardRepository.findByIdAndStateAndHiddenByReportFalseForUpdate(15L, SoftDeleteState.ACTIVE))
                .thenReturn(Optional.of(board));
        when(boardStatsRepository.findTotalViewsByBoardId(15L)).thenReturn(Optional.of(1L));

        long result = viewCounterService.increaseBoardViewAndGet(15L);

        assertThat(result).isEqualTo(1L);
        verify(boardStatsRepository).save(org.mockito.ArgumentMatchers.any(BoardStats.class));
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
    void getBoardViewCount_returnsZeroWhenStatsRowIsMissing() {
        ReflectionTestUtils.setField(viewCounterService, "redisEnabled", false);
        when(boardStatsRepository.findTotalViewsByBoardId(6L)).thenReturn(Optional.empty());

        long result = viewCounterService.getBoardViewCount(6L);

        assertThat(result).isZero();
    }

    @Test
    void getBoardViewCounts_defaultsMissingStatsRowsToZero() {
        ReflectionTestUtils.setField(viewCounterService, "redisEnabled", false);
        when(boardStatsRepository.findViewCountsByBoardIdIn(Set.of(1L, 2L)))
                .thenReturn(List.of(statsRow(1L, 10L)));

        Map<Long, Long> result = viewCounterService.getBoardViewCounts(List.of(1L, 2L));

        assertThat(result).containsEntry(1L, 10L);
        assertThat(result).containsEntry(2L, 0L);
    }

    @Test
    void getBoardViewCounts_propagatesWhenStatsQueryFails() {
        ReflectionTestUtils.setField(viewCounterService, "redisEnabled", false);
        when(boardStatsRepository.findViewCountsByBoardIdIn(Set.of(7L)))
                .thenThrow(new DataAccessResourceFailureException("board_stats missing"));

        assertThatThrownBy(() -> viewCounterService.getBoardViewCounts(List.of(7L)))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessageContaining("board_stats missing");
    }

    @Test
    void getBoardViewCount_propagatesWhenStatsQueryFails() {
        ReflectionTestUtils.setField(viewCounterService, "redisEnabled", false);
        when(boardStatsRepository.findTotalViewsByBoardId(8L))
                .thenThrow(new DataAccessResourceFailureException("board_stats missing"));

        assertThatThrownBy(() -> viewCounterService.getBoardViewCount(8L))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessageContaining("board_stats missing");
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
}
