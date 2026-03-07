package kwh.PublicCookedFood.metrics.popular;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardPopularSnapshotServiceUnitTest {

    @Mock
    private BoardPopularSnapshotRepository snapshotRepository;

    @Mock
    private BoardRepository boardRepository;

    @InjectMocks
    private BoardPopularSnapshotService snapshotService;

    @Test
    void loadFeaturedRankingPage_returnsBoardsInRankingOrder() {
        Pageable pageable = PageRequest.of(0, 10);
        when(snapshotRepository.countActiveVisibleBySlot(
                eq(BoardPopularSnapshotService.FEATURED_RANKING_TYPE),
                eq(""),
                any(),
                eq(SoftDeleteState.ACTIVE)))
                .thenReturn(2L);
        when(snapshotRepository.findActiveVisibleBoardIdsBySlot(
                eq(BoardPopularSnapshotService.FEATURED_RANKING_TYPE),
                eq(""),
                any(),
                eq(SoftDeleteState.ACTIVE),
                eq(pageable)))
                .thenReturn(List.of(2L, 1L));

        Board board1 = Board.builder().id(1L).title("one").state(SoftDeleteState.ACTIVE).hiddenByReport(false).build();
        Board board2 = Board.builder().id(2L).title("two").state(SoftDeleteState.ACTIVE).hiddenByReport(false).build();
        when(boardRepository.findAllByIdInWithAccountAndState(List.of(2L, 1L), SoftDeleteState.ACTIVE))
                .thenReturn(List.of(board1, board2));

        Optional<Page<Board>> result = snapshotService.loadFeaturedRankingPage(pageable, null);

        assertThat(result).isPresent();
        assertThat(result.get().getTotalElements()).isEqualTo(2L);
        assertThat(result.get().getContent()).extracting(Board::getId).containsExactly(2L, 1L);
    }

    @Test
    void loadFeaturedRankingPage_returnsEmptyWhenNoActiveRanking() {
        Pageable pageable = PageRequest.of(0, 10);
        when(snapshotRepository.countActiveVisibleBySlot(
                eq(BoardPopularSnapshotService.FEATURED_RANKING_TYPE),
                eq(""),
                any(),
                eq(SoftDeleteState.ACTIVE)))
                .thenReturn(0L);

        Optional<Page<Board>> result = snapshotService.loadFeaturedRankingPage(pageable, null);

        assertThat(result).isEmpty();
    }

    @Test
    void loadFeaturedRankingPage_returnsEmptyWhenRankingLookupFails() {
        Pageable pageable = PageRequest.of(0, 10);
        when(snapshotRepository.countActiveVisibleBySlot(
                eq(BoardPopularSnapshotService.FEATURED_RANKING_TYPE),
                eq(""),
                any(),
                eq(SoftDeleteState.ACTIVE)))
                .thenThrow(new DataAccessResourceFailureException("snapshot table missing"));

        Optional<Page<Board>> result = snapshotService.loadFeaturedRankingPage(pageable, null);

        assertThat(result).isEmpty();
    }

    @Test
    void loadFeaturedRankingPage_propagatesUnexpectedRuntimeException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(snapshotRepository.countActiveVisibleBySlot(
                eq(BoardPopularSnapshotService.FEATURED_RANKING_TYPE),
                eq(""),
                any(),
                eq(SoftDeleteState.ACTIVE)))
                .thenThrow(new RuntimeException("snapshot mapping bug"));

        assertThatThrownBy(() -> snapshotService.loadFeaturedRankingPage(pageable, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("mapping bug");
    }

    @Test
    void replaceFeaturedRanking_replacesSlotRows() {
        LocalDateTime generatedAt = LocalDateTime.now();
        LocalDateTime expiresAt = generatedAt.plusMinutes(15);

        snapshotService.replaceFeaturedRanking("general", List.of(3L, 4L), generatedAt, expiresAt);

        verify(snapshotRepository).deleteBySlot(BoardPopularSnapshotService.FEATURED_RANKING_TYPE, "general");

        ArgumentCaptor<List<BoardPopularSnapshot>> captor = ArgumentCaptor.forClass(List.class);
        verify(snapshotRepository).saveAll(captor.capture());

        List<BoardPopularSnapshot> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getRankNo()).isEqualTo(1);
        assertThat(saved.get(0).getBoardId()).isEqualTo(3L);
        assertThat(saved.get(1).getRankNo()).isEqualTo(2);
        assertThat(saved.get(1).getBoardId()).isEqualTo(4L);
    }
}
