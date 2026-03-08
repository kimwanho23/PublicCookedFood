package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.metrics.popular.BoardPopularSnapshotService;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardServiceFeaturedSnapshotUnitTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private CommentsRepository commentsRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private BoardSectionService boardSectionService;

    @Mock
    private BoardPolicyService boardPolicyService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BoardPopularSnapshotService boardPopularSnapshotService;

    private BoardService boardService;

    @BeforeEach
    void setUp() {
        boardService = new BoardService(
                boardRepository,
                commentsRepository,
                imageService,
                boardSectionService,
                boardPolicyService,
                accountRepository,
                boardPopularSnapshotService
        );
    }

    @Test
    void getFeaturedBoardList_usesSnapshotWhenNoViewerSpecificFilters() {
        Pageable pageable = PageRequest.of(0, 15);
        Page<Board> snapshotPage = new PageImpl<>(List.of(
                Board.builder().id(1L).title("featured").state(SoftDeleteState.ACTIVE).hiddenByReport(false).build()
        ), pageable, 1);

        when(boardPopularSnapshotService.loadFeaturedRankingPage(pageable, "general"))
                .thenReturn(Optional.of(snapshotPage));

        Page<Board> result = boardService.getFeaturedBoardList(pageable, "general", null, Set.of());

        assertThat(result).isSameAs(snapshotPage);
        verify(boardPopularSnapshotService).loadFeaturedRankingPage(pageable, "general");
        verify(boardRepository, never()).findFeaturedByStateWithAccount(any(), anyBoolean(), anyCollection(), any(), anyLong(), any(), any());
    }

    @Test
    void getFeaturedBoardList_fallsBackToDbWhenSnapshotMissing() {
        Pageable pageable = PageRequest.of(0, 15);
        Page<Board> dbPage = new PageImpl<>(List.of(), pageable, 0);

        when(boardPopularSnapshotService.loadFeaturedRankingPage(pageable, null)).thenReturn(Optional.empty());
        when(boardPolicyService.getFeaturedLikeThreshold()).thenReturn(5);
        when(boardRepository.findFeaturedByStateWithAccount(eq(SoftDeleteState.ACTIVE), eq(false), anyCollection(), isNull(), eq(5L), isNull(), eq(pageable)))
                .thenReturn(dbPage);

        Page<Board> result = boardService.getFeaturedBoardList(pageable, null, null, Set.of());

        assertThat(result).isSameAs(dbPage);
        verify(boardPopularSnapshotService).loadFeaturedRankingPage(pageable, null);
        verify(boardRepository).findFeaturedByStateWithAccount(eq(SoftDeleteState.ACTIVE), eq(false), anyCollection(), isNull(), eq(5L), isNull(), eq(pageable));
    }

    @Test
    void getFeaturedBoardList_fallsBackToDbWhenBlockedFilterExists() {
        Pageable pageable = PageRequest.of(0, 15);
        Page<Board> dbPage = new PageImpl<>(List.of(), pageable, 0);

        when(boardPolicyService.getFeaturedLikeThreshold()).thenReturn(5);
        when(boardRepository.findFeaturedByStateWithAccount(eq(SoftDeleteState.ACTIVE), eq(true), anyCollection(), isNull(), eq(5L), isNull(), eq(pageable)))
                .thenReturn(dbPage);

        Page<Board> result = boardService.getFeaturedBoardList(pageable, null, null, Set.of(100L));

        assertThat(result).isSameAs(dbPage);
        verify(boardPopularSnapshotService, never()).loadFeaturedRankingPage(any(), any());
        verify(boardRepository).findFeaturedByStateWithAccount(eq(SoftDeleteState.ACTIVE), eq(true), anyCollection(), isNull(), eq(5L), isNull(), eq(pageable));
    }

    @Test
    void getFeaturedBoardList_fallsBackToDbWhenSnapshotLoadFails() {
        Pageable pageable = PageRequest.of(0, 15);
        Page<Board> dbPage = new PageImpl<>(List.of(), pageable, 0);

        when(boardPopularSnapshotService.loadFeaturedRankingPage(pageable, null))
                .thenReturn(Optional.empty());
        when(boardPolicyService.getFeaturedLikeThreshold()).thenReturn(5);
        when(boardRepository.findFeaturedByStateWithAccount(eq(SoftDeleteState.ACTIVE), eq(false), anyCollection(), isNull(), eq(5L), isNull(), eq(pageable)))
                .thenReturn(dbPage);

        Page<Board> result = boardService.getFeaturedBoardList(pageable, null, null, Set.of());

        assertThat(result).isSameAs(dbPage);
        verify(boardRepository).findFeaturedByStateWithAccount(eq(SoftDeleteState.ACTIVE), eq(false), anyCollection(), isNull(), eq(5L), isNull(), eq(pageable));
    }

    @Test
    void getFeaturedBoardList_propagatesUnexpectedRuntimeExceptionFromSnapshotLoad() {
        Pageable pageable = PageRequest.of(0, 15);

        when(boardPopularSnapshotService.loadFeaturedRankingPage(pageable, null))
                .thenThrow(new RuntimeException("snapshot mapping bug"));

        assertThatThrownBy(() -> boardService.getFeaturedBoardList(pageable, null, null, Set.of()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("mapping bug");
    }

    @Test
    void getBoardListOrderByViews_fallsBackToBoardViewsSortWhenViewStatsQueryFails() {
        Pageable pageable = PageRequest.of(0, 15);
        Page<Board> dbPage = new PageImpl<>(List.of(), pageable, 0);

        when(boardRepository.findAllByStateWithAccountOrderByViewStats(eq(SoftDeleteState.ACTIVE), eq(false), anyCollection(), isNull(), isNull(), eq(pageable)))
                .thenThrow(new DataAccessResourceFailureException("board_stats join failed"));
        when(boardRepository.findAllByStateWithAccount(eq(SoftDeleteState.ACTIVE), eq(false), anyCollection(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(dbPage);

        Page<Board> result = boardService.getBoardListOrderByViews(pageable, null, null, Set.of());

        assertThat(result).isSameAs(dbPage);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(boardRepository).findAllByStateWithAccount(eq(SoftDeleteState.ACTIVE), eq(false), anyCollection(), isNull(), isNull(), pageableCaptor.capture());
        Pageable fallbackPageable = pageableCaptor.getValue();
        assertThat(fallbackPageable.getSort().getOrderFor("views")).isNotNull();
        assertThat(fallbackPageable.getSort().getOrderFor("views").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(fallbackPageable.getSort().getOrderFor("regTime")).isNotNull();
    }

    @Test
    void getBoardListOrderByViews_propagatesUnexpectedRuntimeException() {
        Pageable pageable = PageRequest.of(0, 15);

        when(boardRepository.findAllByStateWithAccountOrderByViewStats(eq(SoftDeleteState.ACTIVE), eq(false), anyCollection(), isNull(), isNull(), eq(pageable)))
                .thenThrow(new RuntimeException("projection bug"));

        assertThatThrownBy(() -> boardService.getBoardListOrderByViews(pageable, null, null, Set.of()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("projection bug");
    }
}
