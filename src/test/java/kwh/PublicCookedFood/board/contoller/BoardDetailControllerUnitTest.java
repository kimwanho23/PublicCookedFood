package kwh.PublicCookedFood.board.contoller;

import kwh.PublicCookedFood.board.domain.BoardReportReason;
import kwh.PublicCookedFood.board.dto.request.CommentCreateRequest;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import kwh.PublicCookedFood.board.dto.response.CommentResponse;
import kwh.PublicCookedFood.board.facade.BoardDetailFacade;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.domain.Account;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
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
    private BoardDetailFacade boardDetailFacade;

    @InjectMocks
    private BoardDetailController boardDetailController;

    @Test
    void likes_marksSkipViewIncreaseForRedirect() {
        Account account = loginAccount(100L);
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String viewName = boardDetailController.likes(10L, account, null, redirectAttributes);

        verify(boardDetailFacade).toggleLike(10L, 100L);
        assertThat(viewName).isEqualTo("redirect:/boards/10");
        assertThat(redirectAttributes.getFlashAttributes().get("skipViewIncrease"))
                .isEqualTo(Boolean.TRUE);
    }

    @Test
    void boardDetail_increasesViewsByDefault() {
        Pageable pageable = commentPageable(0, 50);
        Model model = new ExtendedModelMap();
        BoardDetailFacade.BoardDetailViewData viewData = detailViewData(pageable);
        MockHttpServletRequest request = new MockHttpServletRequest();

        when(boardDetailFacade.loadBoardDetail(eq(10L), isNull(), eq(pageable), eq(true)))
                .thenReturn(viewData);

        String viewName = boardDetailController.boardDetail(null, 10L, model, 0, 50, request);

        verify(boardDetailFacade).loadBoardDetail(10L, null, pageable, true);
        assertThat(viewName).isEqualTo("/boards/boardDetail");
    }

    @Test
    void boardDetail_skipsViewIncreaseWhenFlagExists() {
        Pageable pageable = commentPageable(0, 50);
        Model model = new ExtendedModelMap();
        BoardDetailFacade.BoardDetailViewData viewData = detailViewData(pageable);
        MockHttpServletRequest request = new MockHttpServletRequest();
        FlashMap flashMap = new FlashMap();
        flashMap.put("skipViewIncrease", true);
        request.setAttribute(DispatcherServlet.INPUT_FLASH_MAP_ATTRIBUTE, flashMap);

        when(boardDetailFacade.loadBoardDetail(eq(10L), isNull(), eq(pageable), eq(false)))
                .thenReturn(viewData);

        String viewName = boardDetailController.boardDetail(null, 10L, model, 0, 50, request);

        verify(boardDetailFacade).loadBoardDetail(10L, null, pageable, false);
        assertThat(viewName).isEqualTo("/boards/boardDetail");
    }

    @Test
    void boardDetail_skipsViewIncreaseWhenCommentPageNavigationOccurs() {
        Pageable pageable = commentPageable(1, 50);
        Model model = new ExtendedModelMap();
        BoardDetailFacade.BoardDetailViewData viewData = detailViewData(pageable);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/boards/10");
        request.addHeader("Referer", "http://localhost/boards/10?commentPage=0&commentSize=50");
        request.addParameter("commentPage", "1");
        request.addParameter("commentSize", "50");

        when(boardDetailFacade.loadBoardDetail(eq(10L), isNull(), eq(pageable), eq(false)))
                .thenReturn(viewData);

        String viewName = boardDetailController.boardDetail(null, 10L, model, 1, 50, request);

        verify(boardDetailFacade).loadBoardDetail(10L, null, pageable, false);
        assertThat(viewName).isEqualTo("/boards/boardDetail");
    }

    @Test
    void addComment_withBindingErrors_marksSkipViewIncrease() {
        Account account = loginAccount(100L);
        CommentCreateRequest request = new CommentCreateRequest();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(request, "comment");
        bindingResult.rejectValue("contents", "NotBlank", "댓글 내용을 확인해주세요.");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String viewName = boardDetailController.addComment(account, 10L, 2, 30, request, bindingResult, redirectAttributes);

        verify(boardDetailFacade, never()).addComment(eq(account), eq(10L), eq(request), eq(30));
        assertThat(viewName).isEqualTo("redirect:/boards/10?commentPage=2&commentSize=30#board-comments");
        assertThat(redirectAttributes.getFlashAttributes().get("commentError"))
                .isEqualTo("댓글 내용을 확인해주세요.");
    }

    @Test
    void deleteScrap_redirectsToSafePathWhenProvided() {
        Account account = loginAccount(100L);
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String viewName = boardDetailController.deleteScrap(10L, account, "/boards/scraps", redirectAttributes);

        verify(boardDetailFacade).removeScrap(10L, 100L);
        assertThat(viewName).isEqualTo("redirect:/boards/scraps");
        assertThat(redirectAttributes.getFlashAttributes().get("skipViewIncrease"))
                .isEqualTo(Boolean.TRUE);
    }

    @Test
    void deleteScrap_fallsBackToBoardDetailWhenRedirectIsUnsafe() {
        Account account = loginAccount(100L);
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String viewName = boardDetailController.deleteScrap(10L, account, "https://evil.example/path", redirectAttributes);

        verify(boardDetailFacade).removeScrap(10L, 100L);
        assertThat(viewName).isEqualTo("redirect:/boards/10");
        assertThat(redirectAttributes.getFlashAttributes().get("skipViewIncrease"))
                .isEqualTo(Boolean.TRUE);
    }

    private BoardDetailFacade.BoardDetailViewData detailViewData(Pageable pageable) {
        BoardDetailResponse boardDetailResponse = BoardDetailResponse.builder()
                .id(10L)
                .title("title")
                .contents("contents")
                .views(1L)
                .build();

        Page<CommentResponse> comments = Page.empty(pageable);
        return new BoardDetailFacade.BoardDetailViewData(
                boardDetailResponse,
                0L,
                0L,
                null,
                false,
                false,
                false,
                false,
                null,
                comments,
                0L,
                BoardReportReason.values()
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
}

