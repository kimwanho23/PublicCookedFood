package kwh.PublicCookedFood.board.service.query;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardPageQuery;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.service.support.BoardVisibilityCriteria;
import kwh.PublicCookedFood.metrics.popular.BoardPopularSnapshotService;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardListQueryServiceUnitTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardPolicyQueryService boardPolicyQueryService;

    @Mock
    private BoardPopularSnapshotService boardPopularSnapshotService;

    private BoardListQueryService boardListQueryService;

    @BeforeEach
    void setUp() {
        boardListQueryService = new BoardListQueryService(
                boardRepository,
                boardPolicyQueryService,
                boardPopularSnapshotService
        );
    }

    @Test
    void loadFeatured_usesSnapshotWhenNoViewerSpecificFilters() {
        Pageable pageable = PageRequest.of(0, 15);
        Page<Board> snapshotPage = new PageImpl<>(List.of(
                Board.builder().id(1L).title("featured").state(SoftDeleteState.ACTIVE).hiddenByReport(false).build()
        ), pageable, 1);

        when(boardPopularSnapshotService.loadFeaturedRankingPage(pageable, "general"))
                .thenReturn(Optional.of(snapshotPage));

        Page<Board> result = boardListQueryService.load(criteria(null, "general", BoardListOrder.RECENT, true, null, pageable, Set.of()));

        assertThat(result).isSameAs(snapshotPage);
        verify(boardPopularSnapshotService).loadFeaturedRankingPage(pageable, "general");
        verify(boardRepository, never()).findPageWithAccount(any());
    }

    @Test
    void loadFeatured_fallsBackToDbWhenSnapshotMissing() {
        Pageable pageable = PageRequest.of(0, 15);
        Page<Board> dbPage = new PageImpl<>(List.of(), pageable, 0);

        when(boardPopularSnapshotService.loadFeaturedRankingPage(pageable, null)).thenReturn(Optional.empty());
        when(boardPolicyQueryService.getFeaturedLikeThreshold()).thenReturn(5);
        when(boardRepository.findPageWithAccount(argThat(query ->
                query != null
                        && query.order() == BoardPageQuery.Order.FEATURED
                        && query.visibility().excludeBlocked() == false
                        && query.featuredThreshold().minimumLikes().map(value -> value == 5L).orElse(false)
                        && query.section().sectionKey().isEmpty()
                        && query.pageable().equals(pageable)
        )))
                .thenReturn(dbPage);

        Page<Board> result = boardListQueryService.load(criteria(null, null, BoardListOrder.RECENT, true, null, pageable, Set.of()));

        assertThat(result).isSameAs(dbPage);
        verify(boardPopularSnapshotService).loadFeaturedRankingPage(pageable, null);
        verify(boardRepository).findPageWithAccount(argThat(query ->
                query != null
                        && query.order() == BoardPageQuery.Order.FEATURED
                        && !query.visibility().excludeBlocked()
                        && query.featuredThreshold().minimumLikes().map(value -> value == 5L).orElse(false)
                        && query.section().sectionKey().isEmpty()
                        && query.pageable().equals(pageable)
        ));
    }

    @Test
    void loadFeatured_fallsBackToDbWhenBlockedFilterExists() {
        Pageable pageable = PageRequest.of(0, 15);
        Page<Board> dbPage = new PageImpl<>(List.of(), pageable, 0);

        when(boardPolicyQueryService.getFeaturedLikeThreshold()).thenReturn(5);
        when(boardRepository.findPageWithAccount(argThat(query ->
                query != null
                        && query.order() == BoardPageQuery.Order.FEATURED
                        && query.visibility().excludeBlocked()
                        && query.visibility().blockedAccountIds().size() == 1
                        && query.visibility().blockedAccountIds().contains(100L)
                        && query.featuredThreshold().minimumLikes().map(value -> value == 5L).orElse(false)
                        && query.pageable().equals(pageable)
        )))
                .thenReturn(dbPage);

        Page<Board> result = boardListQueryService.load(criteria(null, null, BoardListOrder.RECENT, true, null, pageable, Set.of(100L)));

        assertThat(result).isSameAs(dbPage);
        verify(boardPopularSnapshotService, never()).loadFeaturedRankingPage(any(), any());
        verify(boardRepository).findPageWithAccount(argThat(query ->
                query != null
                        && query.order() == BoardPageQuery.Order.FEATURED
                        && query.visibility().excludeBlocked()
                        && query.visibility().blockedAccountIds().size() == 1
                        && query.visibility().blockedAccountIds().contains(100L)
                        && query.featuredThreshold().minimumLikes().map(value -> value == 5L).orElse(false)
                        && query.pageable().equals(pageable)
        ));
    }

    @Test
    void loadFeatured_propagatesUnexpectedRuntimeExceptionFromSnapshotLoad() {
        Pageable pageable = PageRequest.of(0, 15);

        when(boardPopularSnapshotService.loadFeaturedRankingPage(pageable, null))
                .thenThrow(new RuntimeException("snapshot mapping bug"));

        assertThatThrownBy(() -> boardListQueryService.load(criteria(null, null, BoardListOrder.RECENT, true, null, pageable, Set.of())))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("mapping bug");
    }

    @Test
    void loadByViews_fallsBackToRecentSortWhenViewStatsQueryFails() {
        Pageable pageable = PageRequest.of(0, 15);
        Page<Board> dbPage = new PageImpl<>(List.of(), pageable, 0);

        when(boardRepository.findPageWithAccount(argThat(query ->
                query != null
                        && query.order() == BoardPageQuery.Order.STATS_VIEWS
                        && query.pageable().equals(pageable)
        )))
                .thenThrow(new DataAccessResourceFailureException("board_stats join failed"));
        when(boardRepository.findPageWithAccount(argThat(query ->
                query != null && query.order() == BoardPageQuery.Order.RECENT
        )))
                .thenReturn(dbPage);

        Page<Board> result = boardListQueryService.load(criteria(null, null, BoardListOrder.VIEWS, false, null, pageable, Set.of()));

        assertThat(result).isSameAs(dbPage);
        ArgumentCaptor<BoardPageQuery> queryCaptor = ArgumentCaptor.forClass(BoardPageQuery.class);
        verify(boardRepository, times(2)).findPageWithAccount(queryCaptor.capture());
        BoardPageQuery fallbackQuery = queryCaptor.getAllValues().get(1);
        Pageable fallbackPageable = fallbackQuery.pageable();
        assertThat(fallbackQuery.order()).isEqualTo(BoardPageQuery.Order.RECENT);
        assertThat(fallbackPageable.getSort().getOrderFor("regTime")).isNotNull();
        assertThat(fallbackPageable.getSort().getOrderFor("regTime").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void loadByViews_propagatesUnexpectedRuntimeException() {
        Pageable pageable = PageRequest.of(0, 15);

        when(boardRepository.findPageWithAccount(argThat(query ->
                query != null
                        && query.order() == BoardPageQuery.Order.STATS_VIEWS
                        && query.pageable().equals(pageable)
        )))
                .thenThrow(new RuntimeException("projection bug"));

        assertThatThrownBy(() -> boardListQueryService.load(criteria(null, null, BoardListOrder.VIEWS, false, null, pageable, Set.of())))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("projection bug");
    }

    @Test
    void loadByLikes_usesLikeStatsQueryPath() {
        Pageable pageable = PageRequest.of(0, 15);
        Page<Board> dbPage = new PageImpl<>(List.of(), pageable, 0);

        when(boardRepository.findPageWithAccount(argThat(query ->
                query != null
                        && query.order() == BoardPageQuery.Order.STATS_LIKES
                        && query.pageable().equals(pageable)
        )))
                .thenReturn(dbPage);

        Page<Board> result = boardListQueryService.load(criteria(null, null, BoardListOrder.LIKES, false, null, pageable, Set.of()));

        assertThat(result).isSameAs(dbPage);
        verify(boardRepository).findPageWithAccount(argThat(query ->
                query != null
                        && query.order() == BoardPageQuery.Order.STATS_LIKES
                        && query.pageable().equals(pageable)
        ));
    }

    @Test
    void loadByComments_fallsBackToRecentSortWhenStatsQueryFails() {
        Pageable pageable = PageRequest.of(0, 15);
        Page<Board> dbPage = new PageImpl<>(List.of(), pageable, 0);

        when(boardRepository.findPageWithAccount(argThat(query ->
                query != null
                        && query.order() == BoardPageQuery.Order.STATS_COMMENTS
                        && query.pageable().equals(pageable)
        )))
                .thenThrow(new DataAccessResourceFailureException("board_stats join failed"));
        when(boardRepository.findPageWithAccount(argThat(query ->
                query != null && query.order() == BoardPageQuery.Order.RECENT
        )))
                .thenReturn(dbPage);

        Page<Board> result = boardListQueryService.load(criteria(null, null, BoardListOrder.COMMENTS, false, null, pageable, Set.of()));

        assertThat(result).isSameAs(dbPage);
        ArgumentCaptor<BoardPageQuery> queryCaptor = ArgumentCaptor.forClass(BoardPageQuery.class);
        verify(boardRepository, times(2)).findPageWithAccount(queryCaptor.capture());
        BoardPageQuery fallbackQuery = queryCaptor.getAllValues().get(1);
        Pageable fallbackPageable = fallbackQuery.pageable();
        assertThat(fallbackQuery.order()).isEqualTo(BoardPageQuery.Order.RECENT);
        assertThat(fallbackPageable.getSort().getOrderFor("regTime")).isNotNull();
        assertThat(fallbackPageable.getSort().getOrderFor("regTime").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    private BoardListCriteria criteria(String search,
                                       String sectionKey,
                                       BoardListOrder order,
                                       boolean featuredPage,
                                       Long authorId,
                                       Pageable pageable,
                                       Set<Long> blockedAccountIds) {
        return new BoardListCriteria(
                BoardSearchKeyword.from(search)
                        .<BoardListCriteria.SearchFilter>map(BoardListCriteria.SearchFilter::search)
                        .orElseGet(BoardListCriteria.SearchFilter::none),
                BoardSectionKey.from(sectionKey)
                        .<BoardListCriteria.SectionFilter>map(BoardListCriteria.SectionFilter::section)
                        .orElseGet(BoardListCriteria.SectionFilter::all),
                order,
                featuredPage,
                authorId,
                pageable,
                BoardVisibilityCriteria.of(blockedAccountIds)
        );
    }
}
