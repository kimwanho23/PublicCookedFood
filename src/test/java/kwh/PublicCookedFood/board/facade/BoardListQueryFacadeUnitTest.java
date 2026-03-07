package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardThumbnailDisplayMode;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.service.BoardSectionService;
import kwh.PublicCookedFood.board.service.BoardService;
import kwh.PublicCookedFood.common.dto.request.BoardSearchQuery;
import kwh.PublicCookedFood.metrics.view.ViewCounterService;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardListQueryFacadeUnitTest {

    @Mock
    private BoardService boardService;

    @Mock
    private BoardSectionService boardSectionService;

    @Mock
    private AccountBlockService accountBlockService;

    @Mock
    private ViewCounterService viewCounterService;

    @InjectMocks
    private BoardListQueryFacade boardListQueryFacade;

    @Test
    void loadBoardList_resolvesViewsFromViewCounterService() {
        Pageable pageable = PageRequest.of(0, 15);
        BoardSearchQuery query = new BoardSearchQuery();

        Board board = Board.builder()
                .id(10L)
                .title("title")
                .contents("contents")
                .views(5L)
                .likeCount(1L)
                .commentCount(0L)
                .state(SoftDeleteState.ACTIVE)
                .hiddenByReport(false)
                .build();
        Page<Board> boardPage = new PageImpl<>(List.of(board), pageable, 1);

        when(accountBlockService.getViewRestrictedAccountIds(null)).thenReturn(Set.of());
        when(boardService.getBoardList(any(Pageable.class), isNull(), isNull(), anySet())).thenReturn(boardPage);
        when(viewCounterService.getBoardViewCounts(Set.of(10L))).thenReturn(Map.of(10L, 123L));
        when(boardService.getThumbnailDisplayMode()).thenReturn(BoardThumbnailDisplayMode.NONE);
        when(boardSectionService.getActiveSections()).thenReturn(List.of());
        when(boardService.getFeaturedLikeThreshold()).thenReturn(3);

        BoardFacade.BoardListViewData result = boardListQueryFacade.loadBoardList(
                pageable,
                query,
                false,
                false,
                null
        );

        assertThat(result.boardViewMap()).containsEntry(10L, 123L);
        verify(viewCounterService).getBoardViewCounts(Set.of(10L));
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
                .views(1L)
                .likeCount(0L)
                .commentCount(0L)
                .state(SoftDeleteState.ACTIVE)
                .hiddenByReport(false)
                .build();
        Page<Board> boardPage = new PageImpl<>(List.of(board), pageable, 1);

        when(accountBlockService.getViewRestrictedAccountIds(null)).thenReturn(Set.of());
        when(boardService.getBoardListOrderByViews(any(Pageable.class), isNull(), isNull(), anySet())).thenReturn(boardPage);
        when(viewCounterService.getBoardViewCounts(Set.of(11L))).thenReturn(Map.of(11L, 50L));
        when(boardService.getThumbnailDisplayMode()).thenReturn(BoardThumbnailDisplayMode.NONE);
        when(boardSectionService.getActiveSections()).thenReturn(List.of());
        when(boardService.getFeaturedLikeThreshold()).thenReturn(3);

        BoardFacade.BoardListViewData result = boardListQueryFacade.loadBoardList(
                pageable,
                query,
                false,
                false,
                null
        );

        assertThat(result.orderBy()).isEqualTo("views");
        assertThat(result.boardViewMap()).containsEntry(11L, 50L);
        verify(boardService).getBoardListOrderByViews(any(Pageable.class), isNull(), isNull(), anySet());
        verify(boardService, never()).getBoardList(any(Pageable.class), isNull(), isNull(), anySet());
    }
}

