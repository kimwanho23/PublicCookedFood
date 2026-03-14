package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.application.query.view.CommentNodeView;
import kwh.PublicCookedFood.board.error.BoardErrorCode;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import kwh.PublicCookedFood.board.policy.BoardAuthorizationPolicy;
import kwh.PublicCookedFood.board.service.BoardCounterService;
import kwh.PublicCookedFood.board.service.BoardCounters;
import kwh.PublicCookedFood.board.service.BoardScrapService;
import kwh.PublicCookedFood.board.service.comment.CommentQueryService;
import kwh.PublicCookedFood.board.service.query.BoardDetailQueryService;
import kwh.PublicCookedFood.common.error.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardDetailQueryFacadeUnitTest {

    @Mock
    private BoardDetailQueryService boardDetailQueryService;

    @Mock
    private CommentQueryService commentQueryService;

    @Mock
    private BoardScrapService boardScrapService;

    @Mock
    private BoardAuthorizationPolicy boardAuthorizationPolicy;

    @Mock
    private BoardCounterService boardCounterService;

    @Mock
    private BoardInteractionStateResolver boardInteractionStateResolver;

    private BoardDetailQueryFacade boardDetailQueryFacade;

    @BeforeEach
    void setUp() {
        boardDetailQueryFacade = new BoardDetailQueryFacade(
                boardDetailQueryService,
                commentQueryService,
                boardAuthorizationPolicy,
                boardCounterService,
                boardInteractionStateResolver,
                new BoardDetailReadModelFactory(),
                boardScrapService
        );
    }

    @Test
    void loadBoardDetail_increasesViewsWhenFlagIsTrue() {
        Long boardId = 7L;
        Pageable pageable = PageRequest.of(0, 20);
        BoardDetailResponse beforeIncrease = detail(boardId);
        Page<CommentNodeView> emptyComments = Page.empty(pageable);

        when(boardDetailQueryService.getBoardDetail(boardId)).thenReturn(beforeIncrease);
        when(boardAuthorizationPolicy.isViewRestricted(eq(BoardViewer.anonymous()), eq(1L))).thenReturn(false);
        when(boardCounterService.getDetailCounters(boardId, BoardViewer.anonymous(), true)).thenReturn(new BoardCounters(101L, 0L, 0L));
        when(boardScrapService.getScrapCount(boardId)).thenReturn(0L);
        when(boardInteractionStateResolver.resolve(boardId, BoardViewer.anonymous(), 1L))
                .thenReturn(BoardInteractionState.anonymous());
        when(commentQueryService.getCommentListWithReplies(eq(boardId), eq(pageable), eq(BoardViewer.anonymous()))).thenReturn(emptyComments);

        boardDetailQueryFacade.loadBoardDetail(boardId, null, pageable, true);

        verify(boardCounterService, times(1)).getDetailCounters(boardId, BoardViewer.anonymous(), true);
        verify(boardDetailQueryService, times(1)).getBoardDetail(boardId);
    }

    @Test
    void loadBoardDetail_skipsViewIncreaseWhenFlagIsFalse() {
        Long boardId = 8L;
        Pageable pageable = PageRequest.of(0, 20);
        BoardDetailResponse board = detail(boardId);
        Page<CommentNodeView> emptyComments = Page.empty(pageable);

        when(boardDetailQueryService.getBoardDetail(boardId)).thenReturn(board);
        when(boardAuthorizationPolicy.isViewRestricted(eq(BoardViewer.anonymous()), eq(1L))).thenReturn(false);
        when(boardCounterService.getDetailCounters(boardId, BoardViewer.anonymous(), false)).thenReturn(new BoardCounters(100L, 0L, 0L));
        when(boardScrapService.getScrapCount(boardId)).thenReturn(0L);
        when(boardInteractionStateResolver.resolve(boardId, BoardViewer.anonymous(), 1L))
                .thenReturn(BoardInteractionState.anonymous());
        when(commentQueryService.getCommentListWithReplies(eq(boardId), eq(pageable), eq(BoardViewer.anonymous()))).thenReturn(emptyComments);

        boardDetailQueryFacade.loadBoardDetail(boardId, null, pageable, false);

        verify(boardCounterService, times(1)).getDetailCounters(boardId, BoardViewer.anonymous(), false);
        verify(boardDetailQueryService, times(1)).getBoardDetail(boardId);
    }

    @Test
    void loadBoardDetail_throwsAppExceptionWhenViewIsBlocked() {
        Long boardId = 9L;
        Pageable pageable = PageRequest.of(0, 20);
        BoardDetailResponse board = detail(boardId);

        when(boardDetailQueryService.getBoardDetail(boardId)).thenReturn(board);
        when(boardAuthorizationPolicy.isViewRestricted(eq(BoardViewer.anonymous()), eq(1L))).thenReturn(true);

        assertThatThrownBy(() -> boardDetailQueryFacade.loadBoardDetail(boardId, null, pageable, true))
                .isInstanceOfSatisfying(AppException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(BoardErrorCode.BOARD_VIEW_BLOCKED));

        verify(boardCounterService, never()).getDetailCounters(anyLong(), any(), eq(true));
    }

    private BoardDetailResponse detail(Long boardId) {
        return BoardDetailResponse.builder()
                .id(boardId)
                .accountId(1L)
                .build();
    }
}

