package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.application.query.BoardCardViewAssembler;
import kwh.PublicCookedFood.board.application.query.view.BoardCardView;
import kwh.PublicCookedFood.board.application.query.view.BoardListPageView;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardThumbnailDisplayMode;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.dto.request.BoardSearchQuery;
import kwh.PublicCookedFood.board.facade.BoardViewer;
import kwh.PublicCookedFood.board.service.query.BoardListCriteria;
import kwh.PublicCookedFood.board.service.query.BoardListCriteriaResolver;
import kwh.PublicCookedFood.board.service.query.BoardListOrder;
import kwh.PublicCookedFood.board.service.query.BoardListQueryService;
import kwh.PublicCookedFood.board.service.query.BoardPolicyQueryService;
import kwh.PublicCookedFood.board.service.query.BoardSectionQueryService;
import kwh.PublicCookedFood.board.service.support.BoardThumbnailExtractor;
import kwh.PublicCookedFood.board.service.support.BoardStatsSummary;
import kwh.PublicCookedFood.board.service.support.BoardStatsSummaryResolver;
import kwh.PublicCookedFood.board.service.support.BoardVisibilityCriteria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardListQueryFacadeUnitTest {

    @Mock
    private BoardListCriteriaResolver boardListCriteriaResolver;

    @Mock
    private BoardThumbnailExtractor boardThumbnailExtractor;

    @Mock
    private BoardStatsSummaryResolver boardStatsSummaryResolver;

    @Mock
    private BoardSectionQueryService boardSectionQueryService;

    @Mock
    private BoardPolicyQueryService boardPolicyQueryService;

    @Mock
    private BoardListQueryService boardListQueryService;

    private BoardListQueryFacade boardListQueryFacade;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        boardListQueryFacade = new BoardListQueryFacade(
                boardListCriteriaResolver,
                boardThumbnailExtractor,
                boardStatsSummaryResolver,
                boardSectionQueryService,
                boardPolicyQueryService,
                boardListQueryService,
                new BoardCardViewAssembler()
        );
    }

    @Test
    void loadBoardList_resolvesViewsFromViewCounterService() {
        Pageable pageable = PageRequest.of(0, 15);
        BoardSearchQuery query = new BoardSearchQuery();

        Board board = Board.builder()
                .id(10L)
                .title("title")
                .contents("contents")
                .state(SoftDeleteState.ACTIVE)
                .hiddenByReport(false)
                .build();
        Page<Board> boardPage = new PageImpl<>(List.of(board), pageable, 1);
        BoardListCriteria criteria = new BoardListCriteria(
                BoardListCriteria.SearchFilter.none(),
                BoardListCriteria.SectionFilter.all(),
                BoardListOrder.RECENT,
                false,
                null,
                pageable,
                BoardVisibilityCriteria.of(null)
        );

        when(boardListCriteriaResolver.resolve(pageable, null, null, null, false, false, BoardViewer.anonymous()))
                .thenReturn(new BoardListCriteriaResolver.Resolution(criteria, null));
        when(boardListQueryService.load(criteria)).thenReturn(boardPage);
        when(boardThumbnailExtractor.extract(boardPage.getContent()))
                .thenReturn(BoardThumbnailExtractor.ThumbnailData.of(Map.of(), Map.of(10L, false)));
        when(boardStatsSummaryResolver.resolve(boardPage.getContent()))
                .thenReturn(Map.of(10L, new BoardStatsSummary(123L, 1L, 0L)));
        when(boardPolicyQueryService.getThumbnailDisplayMode()).thenReturn(BoardThumbnailDisplayMode.NONE);
        when(boardSectionQueryService.getActiveSections()).thenReturn(List.of());
        when(boardPolicyQueryService.getFeaturedLikeThreshold()).thenReturn(3);

        BoardListPageView result = boardListQueryFacade.loadBoardList(
                pageable,
                query,
                false,
                false,
                BoardViewer.anonymous()
        );

        BoardCardView card = result.boardList().getContent().get(0);
        assertThat(card.boardId()).isEqualTo(10L);
        assertThat(card.views()).isEqualTo(123L);
        assertThat(card.likes()).isEqualTo(1L);
        assertThat(card.commentsCount()).isZero();
        verify(boardStatsSummaryResolver).resolve(boardPage.getContent());
    }

    @Test
    void loadBoardList_withViewsOrder_usesViewStatsQueryPath() {
        Pageable pageable = PageRequest.of(0, 15);
        BoardSearchQuery query = new BoardSearchQuery();
        query.setOrderBy("views");

        Board board = Board.builder()
                .id(11L)
                .title("views-title")
                .contents("contents")
                .state(SoftDeleteState.ACTIVE)
                .hiddenByReport(false)
                .build();
        Page<Board> boardPage = new PageImpl<>(List.of(board), pageable, 1);
        BoardListCriteria criteria = new BoardListCriteria(
                BoardListCriteria.SearchFilter.none(),
                BoardListCriteria.SectionFilter.all(),
                BoardListOrder.VIEWS,
                false,
                null,
                pageable,
                BoardVisibilityCriteria.of(null)
        );

        when(boardListCriteriaResolver.resolve(pageable, null, null, "views", false, false, BoardViewer.anonymous()))
                .thenReturn(new BoardListCriteriaResolver.Resolution(criteria, null));
        when(boardListQueryService.load(criteria)).thenReturn(boardPage);
        when(boardThumbnailExtractor.extract(boardPage.getContent()))
                .thenReturn(BoardThumbnailExtractor.ThumbnailData.of(Map.of(), Map.of(11L, false)));
        when(boardStatsSummaryResolver.resolve(boardPage.getContent()))
                .thenReturn(Map.of(11L, new BoardStatsSummary(50L, 0L, 0L)));
        when(boardPolicyQueryService.getThumbnailDisplayMode()).thenReturn(BoardThumbnailDisplayMode.NONE);
        when(boardSectionQueryService.getActiveSections()).thenReturn(List.of());
        when(boardPolicyQueryService.getFeaturedLikeThreshold()).thenReturn(3);

        BoardListPageView result = boardListQueryFacade.loadBoardList(
                pageable,
                query,
                false,
                false,
                BoardViewer.anonymous()
        );

        BoardCardView card = result.boardList().getContent().get(0);
        assertThat(result.orderBy()).isEqualTo("views");
        assertThat(card.boardId()).isEqualTo(11L);
        assertThat(card.views()).isEqualTo(50L);
        verify(boardListQueryService).load(criteria);
    }
}

