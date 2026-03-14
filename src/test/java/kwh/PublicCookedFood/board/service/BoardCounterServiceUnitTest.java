package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.board.repository.LikesRepository;
import kwh.PublicCookedFood.board.facade.BoardViewer;
import kwh.PublicCookedFood.board.service.comment.CommentQueryService;
import kwh.PublicCookedFood.board.repository.BoardStatsRepository;
import kwh.PublicCookedFood.board.service.BoardStatsMutationService;
import kwh.PublicCookedFood.board.service.BoardViewCounterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardCounterServiceUnitTest {

    @Mock
    private CommentsRepository commentsRepository;

    @Mock
    private LikesRepository likesRepository;

    @Mock
    private CommentQueryService commentQueryService;

    @Mock
    private BoardStatsRepository boardStatsRepository;

    @Mock
    private BoardViewCounterService viewCounterService;

    @Mock
    private BoardStatsMutationService boardStatsMutationService;

    @InjectMocks
    private BoardCounterService boardCounterService;

    @Test
    void increaseViewsAndGet_delegatesToViewCounterService() {
        boardCounterService.increaseViewsAndGet(10L);

        verify(viewCounterService).increaseBoardViewAndGet(10L);
    }

    @Test
    void getViews_resolvesBatchCountsFromViewCounterService() {
        when(viewCounterService.getBoardViewCounts(List.of(1L, 2L))).thenReturn(Map.of(1L, 3L, 2L, 4L));

        boardCounterService.getViews(List.of(1L, 2L));

        verify(viewCounterService).getBoardViewCounts(List.of(1L, 2L));
    }

    @Test
    void getLikes_prefersBoardStats() {
        when(boardStatsRepository.findTotalLikesByBoardId(21L)).thenReturn(java.util.Optional.of(9L));

        boardCounterService.getLikes(21L);

        verify(boardStatsRepository).findTotalLikesByBoardId(21L);
    }

    @Test
    void getLikes_fallsBackToLikesRepositoryWhenStatsMissing() {
        when(boardStatsRepository.findTotalLikesByBoardId(22L)).thenReturn(java.util.Optional.empty());
        when(likesRepository.countByBoardId(22L)).thenReturn(4L);

        boardCounterService.getLikes(22L);

        verify(boardStatsRepository).findTotalLikesByBoardId(22L);
        verify(likesRepository).countByBoardId(22L);
    }

    @Test
    void getCommentCount_delegatesToCommentQueryService() {
        when(commentQueryService.getCommentsCount(23L, BoardViewer.authenticated(7L))).thenReturn(6L);

        boardCounterService.getCommentCount(23L, BoardViewer.authenticated(7L));

        verify(commentQueryService).getCommentsCount(23L, BoardViewer.authenticated(7L));
    }

    @Test
    void getDetailCounters_readsViewsLikesAndCommentsThroughUnifiedPath() {
        when(viewCounterService.increaseBoardViewAndGet(31L)).thenReturn(12L);
        when(boardStatsRepository.findTotalLikesByBoardId(31L)).thenReturn(Optional.of(5L));
        when(commentQueryService.getCommentsCount(31L, BoardViewer.authenticated(7L))).thenReturn(9L);

        BoardCounters counters = boardCounterService.getDetailCounters(31L, BoardViewer.authenticated(7L), true);

        assertThat(counters.views()).isEqualTo(12L);
        assertThat(counters.likes()).isEqualTo(5L);
        assertThat(counters.commentsCount()).isEqualTo(9L);
    }

    @Test
    void refreshLikes_syncsBoardStatsOnly() {
        when(likesRepository.countByBoardId(25L)).thenReturn(5L);

        boardCounterService.refreshLikes(25L);

        verify(likesRepository).countByBoardId(25L);
        verify(boardStatsMutationService).syncLikeCount(25L, 5L);
    }

    @Test
    void refreshCommentCount_recomputesActiveCommentCountAndSyncsStats() {
        when(commentsRepository.countByBoardIdAndState(30L, SoftDeleteState.ACTIVE)).thenReturn(7L);

        boardCounterService.refreshCommentCount(30L);

        verify(commentsRepository).countByBoardIdAndState(30L, SoftDeleteState.ACTIVE);
        verify(boardStatsMutationService).syncCommentCount(30L, 7L);
    }
}
