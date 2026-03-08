package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.error.BoardErrorCode;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import kwh.PublicCookedFood.board.dto.response.CommentResponse;
import kwh.PublicCookedFood.board.policy.BoardAuthorizationPolicy;
import kwh.PublicCookedFood.board.service.BoardReportService;
import kwh.PublicCookedFood.board.service.BoardScrapService;
import kwh.PublicCookedFood.board.service.BoardService;
import kwh.PublicCookedFood.board.service.CommentsService;
import kwh.PublicCookedFood.board.service.LikeService;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.metrics.view.ViewCounterService;
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
    private BoardService boardService;

    @Mock
    private CommentsService commentsService;

    @Mock
    private LikeService likeService;

    @Mock
    private BoardScrapService boardScrapService;

    @Mock
    private BoardReportService boardReportService;

    @Mock
    private BoardAuthorizationPolicy boardAuthorizationPolicy;

    @Mock
    private ViewCounterService viewCounterService;

    private BoardDetailQueryFacade boardDetailQueryFacade;

    @BeforeEach
    void setUp() {
        boardDetailQueryFacade = new BoardDetailQueryFacade(
                boardService,
                commentsService,
                likeService,
                boardScrapService,
                boardReportService,
                boardAuthorizationPolicy,
                viewCounterService
        );
    }

    @Test
    void loadBoardDetail_increasesViewsWhenFlagIsTrue() {
        Long boardId = 7L;
        Pageable pageable = PageRequest.of(0, 20);
        BoardDetailResponse beforeIncrease = detail(boardId, 100L);
        Page<CommentResponse> emptyComments = Page.empty(pageable);

        when(boardService.getBoardDetail(boardId)).thenReturn(beforeIncrease);
        when(boardAuthorizationPolicy.isViewRestricted(any(), eq(1L))).thenReturn(false);
        when(viewCounterService.increaseBoardViewAndGet(boardId)).thenReturn(101L);
        when(likeService.getLike(boardId)).thenReturn(0L);
        when(boardScrapService.getScrapCount(boardId)).thenReturn(0L);
        when(commentsService.getCommentListWithReplies(eq(boardId), eq(pageable), isNull())).thenReturn(emptyComments);
        when(commentsService.getCommentsCount(boardId, null)).thenReturn(0L);

        boardDetailQueryFacade.loadBoardDetail(boardId, null, pageable, true);

        verify(viewCounterService, times(1)).increaseBoardViewAndGet(boardId);
        verify(viewCounterService, never()).getBoardViewCount(anyLong());
        verify(boardService, times(1)).getBoardDetail(boardId);
    }

    @Test
    void loadBoardDetail_skipsViewIncreaseWhenFlagIsFalse() {
        Long boardId = 8L;
        Pageable pageable = PageRequest.of(0, 20);
        BoardDetailResponse board = detail(boardId, 100L);
        Page<CommentResponse> emptyComments = Page.empty(pageable);

        when(boardService.getBoardDetail(boardId)).thenReturn(board);
        when(boardAuthorizationPolicy.isViewRestricted(any(), eq(1L))).thenReturn(false);
        when(viewCounterService.getBoardViewCount(boardId)).thenReturn(100L);
        when(likeService.getLike(boardId)).thenReturn(0L);
        when(boardScrapService.getScrapCount(boardId)).thenReturn(0L);
        when(commentsService.getCommentListWithReplies(eq(boardId), eq(pageable), isNull())).thenReturn(emptyComments);
        when(commentsService.getCommentsCount(boardId, null)).thenReturn(0L);

        boardDetailQueryFacade.loadBoardDetail(boardId, null, pageable, false);

        verify(viewCounterService, never()).increaseBoardViewAndGet(anyLong());
        verify(viewCounterService, times(1)).getBoardViewCount(boardId);
        verify(boardService, times(1)).getBoardDetail(boardId);
    }

    @Test
    void loadBoardDetail_throwsAppExceptionWhenViewIsBlocked() {
        Long boardId = 9L;
        Pageable pageable = PageRequest.of(0, 20);
        BoardDetailResponse board = detail(boardId, 50L);

        when(boardService.getBoardDetail(boardId)).thenReturn(board);
        when(boardAuthorizationPolicy.isViewRestricted(any(), eq(1L))).thenReturn(true);

        assertThatThrownBy(() -> boardDetailQueryFacade.loadBoardDetail(boardId, null, pageable, true))
                .isInstanceOfSatisfying(AppException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(BoardErrorCode.BOARD_VIEW_BLOCKED));

        verify(viewCounterService, never()).increaseBoardViewAndGet(anyLong());
        verify(viewCounterService, never()).getBoardViewCount(anyLong());
    }

    private BoardDetailResponse detail(Long boardId, Long views) {
        return BoardDetailResponse.builder()
                .id(boardId)
                .accountId(1L)
                .views(views)
                .build();
    }
}

