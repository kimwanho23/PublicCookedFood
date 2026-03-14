package kwh.PublicCookedFood.board.controller;

import kwh.PublicCookedFood.board.application.query.view.BoardDetailActionsView;
import kwh.PublicCookedFood.board.application.query.view.BoardDetailCountersView;
import kwh.PublicCookedFood.board.application.query.view.BoardDetailPageView;
import kwh.PublicCookedFood.board.application.query.view.CommentNodeView;
import kwh.PublicCookedFood.board.application.query.view.CommentThreadPageView;
import kwh.PublicCookedFood.board.controller.support.BoardDetailNavigationSupport;
import kwh.PublicCookedFood.board.controller.support.BoardViewerSupport;
import kwh.PublicCookedFood.board.domain.BoardReportReason;
import kwh.PublicCookedFood.board.dto.request.CommentCreateRequest;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import kwh.PublicCookedFood.board.facade.BoardDetailFacade;
import kwh.PublicCookedFood.board.facade.BoardDetailQueryFacade;
import kwh.PublicCookedFood.board.facade.BoardInteractionState;
import kwh.PublicCookedFood.board.policy.BoardViewPolicy;
import kwh.PublicCookedFood.board.service.comment.CommentDeleteCommand;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.domain.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardDetailControllerUnitTest {

    @Mock
    private BoardDetailQueryFacade boardDetailQueryFacade;

    @Mock
    private BoardDetailFacade boardDetailFacade;

    @Mock
    private BoardViewPolicy boardViewPolicy;

    private BoardDetailController boardDetailController;
    private BoardDetailNavigationSupport boardDetailNavigationSupport;
    private BoardViewerSupport boardViewerSupport;

    @BeforeEach
    void setUp() {
        boardDetailNavigationSupport = new BoardDetailNavigationSupport();
        boardViewerSupport = new BoardViewerSupport();
        boardDetailController = new BoardDetailController(
                boardDetailQueryFacade,
                boardDetailFacade,
                boardDetailNavigationSupport,
                boardViewerSupport,
                boardViewPolicy
        );
    }

    @Test
    void likes_marksSkipViewIncreaseForRedirect() {
        Account account = loginAccount(100L);
        MockHttpServletRequest request = authenticatedRequest();

        String viewName = boardDetailController.likes(10L, account, null, request);

        verify(boardDetailFacade).toggleLike(10L, 100L);
        verify(boardViewPolicy).markSkipNextDetailView(request, 10L);
        assertThat(viewName).isEqualTo("redirect:/boards/10");
    }

    @Test
    void boardDetail_increasesViewsByDefault() {
        Pageable pageable = commentPageable(0, 50);
        Model model = new ExtendedModelMap();
        BoardDetailPageView viewData = detailViewData(pageable);
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(boardViewPolicy.shouldIncreaseDetailView(request, 10L)).thenReturn(true);

        when(boardDetailQueryFacade.loadBoardDetail(eq(10L), isNull(), eq(pageable), eq(true)))
                .thenReturn(viewData);

        String viewName = boardDetailController.boardDetail(null, 10L, model, 0, 50, request);

        verify(boardDetailQueryFacade).loadBoardDetail(10L, null, pageable, true);
        assertThat(model.getAttribute("view")).isEqualTo(viewData);
        assertThat(viewName).isEqualTo("boards/boardDetail");
    }

    @Test
    void boardDetail_skipsViewIncreaseWhenBoardWasRecentlyViewed() {
        Pageable pageable = commentPageable(0, 50);
        Model model = new ExtendedModelMap();
        BoardDetailPageView viewData = detailViewData(pageable);
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(boardViewPolicy.shouldIncreaseDetailView(request, 10L)).thenReturn(false);

        when(boardDetailQueryFacade.loadBoardDetail(eq(10L), isNull(), eq(pageable), eq(false)))
                .thenReturn(viewData);

        String viewName = boardDetailController.boardDetail(null, 10L, model, 0, 50, request);

        verify(boardDetailQueryFacade).loadBoardDetail(10L, null, pageable, false);
        assertThat(viewName).isEqualTo("boards/boardDetail");
    }

    @Test
    void boardDetail_skipsViewIncreaseWhenCommentPageNavigationOccurs() {
        Pageable pageable = commentPageable(1, 50);
        Model model = new ExtendedModelMap();
        BoardDetailPageView viewData = detailViewData(pageable);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/boards/10");
        request.addHeader("Referer", "http://localhost/boards/10?commentPage=0&commentSize=50");
        request.addParameter("commentPage", "1");
        request.addParameter("commentSize", "50");

        when(boardDetailQueryFacade.loadBoardDetail(eq(10L), isNull(), eq(pageable), eq(false)))
                .thenReturn(viewData);

        String viewName = boardDetailController.boardDetail(null, 10L, model, 1, 50, request);

        verify(boardDetailQueryFacade).loadBoardDetail(10L, null, pageable, false);
        verify(boardViewPolicy, never()).shouldIncreaseDetailView(request, 10L);
        assertThat(viewName).isEqualTo("boards/boardDetail");
    }

    @Test
    void addComment_withBindingErrors_marksSkipViewIncrease() {
        Account account = loginAccount(100L);
        CommentCreateRequest request = new CommentCreateRequest();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(request, "comment");
        bindingResult.rejectValue("contents", "NotBlank", "댓글 내용을 확인해주세요.");
        MockHttpServletRequest httpRequest = authenticatedRequest();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String viewName = boardDetailController.addComment(account, 10L, 2, 30, request, bindingResult, httpRequest, redirectAttributes);

        verify(boardDetailFacade, never()).addComment(org.mockito.ArgumentMatchers.any(), eq(30));
        verify(boardViewPolicy).markSkipNextDetailView(httpRequest, 10L);
        assertThat(viewName).isEqualTo("redirect:/boards/10?commentPage=2&commentSize=30#board-comments");
        assertThat(redirectAttributes.getFlashAttributes().get("commentError"))
                .isEqualTo("댓글 내용을 확인해주세요.");
    }

    @Test
    void addComment_redirectsToTypedResultTargetWhenSuccess() {
        Account account = loginAccount(100L);
        CommentCreateRequest request = new CommentCreateRequest();
        request.setContents("comment");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(request, "comment");
        MockHttpServletRequest httpRequest = authenticatedRequest();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        when(boardDetailFacade.addComment(org.mockito.ArgumentMatchers.any(), eq(30)))
                .thenReturn(BoardDetailFacade.OperationResult.successRedirect("/boards/10?commentPage=0&commentSize=30#comment-88"));

        String viewName = boardDetailController.addComment(account, 10L, 2, 30, request, bindingResult, httpRequest, redirectAttributes);

        verify(boardViewPolicy).markSkipNextDetailView(httpRequest, 10L);
        assertThat(viewName).isEqualTo("redirect:/boards/10?commentPage=0&commentSize=30#comment-88");
    }

    @Test
    void deleteScrap_redirectsToSafePathWhenProvided() {
        Account account = loginAccount(100L);
        MockHttpServletRequest request = authenticatedRequest();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String viewName = boardDetailController.deleteScrap(10L, account, "/boards/scraps", request, redirectAttributes);

        verify(boardDetailFacade).removeScrap(10L, 100L);
        verify(boardViewPolicy, never()).markSkipNextDetailView(request, 10L);
        assertThat(viewName).isEqualTo("redirect:/boards/scraps");
    }

    @Test
    void deleteComment_usesTypedDeleteCommand() {
        Account account = loginAccount(100L);
        MockHttpServletRequest request = authenticatedRequest();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        when(boardDetailFacade.deleteComment(CommentDeleteCommand.of(100L, 10L, 20L)))
                .thenReturn(BoardDetailFacade.OperationResult.success("댓글이 삭제되었습니다."));

        String viewName = boardDetailController.deleteComment(account, 10L, 20L, 1, 30, request, redirectAttributes);

        verify(boardDetailFacade).deleteComment(CommentDeleteCommand.of(100L, 10L, 20L));
        verify(boardViewPolicy).markSkipNextDetailView(request, 10L);
        assertThat(viewName).isEqualTo("redirect:/boards/10?commentPage=1&commentSize=30#board-comments");
    }

    @Test
    void deleteScrap_fallsBackToBoardDetailWhenRedirectIsUnsafe() {
        Account account = loginAccount(100L);
        MockHttpServletRequest request = authenticatedRequest();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String viewName = boardDetailController.deleteScrap(10L, account, "https://evil.example/path", request, redirectAttributes);

        verify(boardDetailFacade).removeScrap(10L, 100L);
        verify(boardViewPolicy).markSkipNextDetailView(request, 10L);
        assertThat(viewName).isEqualTo("redirect:/boards/10");
    }

    @Test
    void deleteScrap_marksSkipWhenRedirectReturnsToBoardDetail() {
        Account account = loginAccount(100L);
        MockHttpServletRequest request = authenticatedRequest();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String viewName = boardDetailController.deleteScrap(
                10L,
                account,
                "/boards/10?commentPage=0&commentSize=50#board-comments",
                request,
                redirectAttributes
        );

        verify(boardDetailFacade).removeScrap(10L, 100L);
        verify(boardViewPolicy).markSkipNextDetailView(request, 10L);
        assertThat(viewName).isEqualTo("redirect:/boards/10?commentPage=0&commentSize=50#board-comments");
    }

    private BoardDetailPageView detailViewData(Pageable pageable) {
        BoardDetailResponse boardDetailResponse = BoardDetailResponse.builder()
                .id(10L)
                .title("title")
                .contents("contents")
                .build();

        Page<CommentNodeView> comments = Page.empty(pageable);
        return new BoardDetailPageView(
                boardDetailResponse,
                new BoardDetailCountersView(0L, 0L, 0L),
                BoardDetailActionsView.from(BoardInteractionState.anonymous()),
                new CommentThreadPageView(10L, null, false, comments),
                java.util.List.of(BoardReportReason.values())
        );
    }

    private Account loginAccount(Long accountId) {
        return Account.builder()
                .id(accountId)
                .email("viewer@test.com")
                .name("viewer")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }

    private Pageable commentPageable(int commentPage, int commentSize) {
        return PageRequest.of(commentPage, commentSize, Sort.by(Sort.Order.asc("regTime")));
    }

    private MockHttpServletRequest authenticatedRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new MockHttpSession());
        return request;
    }
}

