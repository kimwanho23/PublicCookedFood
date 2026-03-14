package kwh.PublicCookedFood.metrics.view;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardStats;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardStatsRepository;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.board.repository.LikesRepository;
import kwh.PublicCookedFood.board.service.BoardStatsMutationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardStatsMutationServiceUnitTest {

    @Mock
    private BoardStatsRepository boardStatsRepository;

    @Mock
    private LikesRepository likesRepository;

    @Mock
    private CommentsRepository commentsRepository;

    @InjectMocks
    private BoardStatsMutationService boardStatsMutationService;

    @Test
    void initializeBoard_createsStatsRowWhenMissing() {
        Board board = Board.builder()
                .id(15L)
                .build();
        when(boardStatsRepository.existsById(15L)).thenReturn(false);

        boardStatsMutationService.initializeBoard(board);

        ArgumentCaptor<BoardStats> captor = ArgumentCaptor.forClass(BoardStats.class);
        verify(boardStatsRepository).save(captor.capture());
        BoardStats saved = captor.getValue();
        assertThat(saved.getBoardId()).isEqualTo(15L);
        assertThat(saved.getTotalViews()).isZero();
        assertThat(saved.getTotalLikes()).isZero();
        assertThat(saved.getTotalComments()).isZero();
    }

    @Test
    void initializeBoard_skipsWhenStatsRowAlreadyExists() {
        Board board = Board.builder()
                .id(16L)
                .build();
        when(boardStatsRepository.existsById(16L)).thenReturn(true);

        boardStatsMutationService.initializeBoard(board);

        verify(boardStatsRepository, never()).save(any(BoardStats.class));
    }

    @Test
    void syncLikeCount_updatesExistingStats() {
        when(boardStatsRepository.updateTotalLikes(10L, 3L)).thenReturn(1);

        boardStatsMutationService.syncLikeCount(10L, 3L);

        verify(boardStatsRepository).updateTotalLikes(10L, 3L);
        verify(boardStatsRepository, never()).save(any(BoardStats.class));
    }

    @Test
    void syncLikeCount_createsStatsWhenMissing() {
        when(boardStatsRepository.updateTotalLikes(11L, 4L)).thenReturn(0);
        when(commentsRepository.countByBoardIdAndState(11L, SoftDeleteState.ACTIVE)).thenReturn(2L);

        boardStatsMutationService.syncLikeCount(11L, 4L);

        ArgumentCaptor<BoardStats> captor = ArgumentCaptor.forClass(BoardStats.class);
        verify(boardStatsRepository).save(captor.capture());
        BoardStats saved = captor.getValue();
        assertThat(saved.getBoardId()).isEqualTo(11L);
        assertThat(saved.getTotalViews()).isZero();
        assertThat(saved.getTotalLikes()).isEqualTo(4L);
        assertThat(saved.getTotalComments()).isEqualTo(2L);
        assertThat(saved.getScore()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void syncCommentCount_retriesWhenConcurrentInsertHappens() {
        when(boardStatsRepository.updateTotalComments(12L, 5L)).thenReturn(0, 1);
        when(likesRepository.countByBoardId(12L)).thenReturn(4L);
        when(boardStatsRepository.save(any(BoardStats.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        boardStatsMutationService.syncCommentCount(12L, 5L);

        verify(boardStatsRepository, times(2)).updateTotalComments(12L, 5L);
        verify(boardStatsRepository).save(any(BoardStats.class));
    }
}
