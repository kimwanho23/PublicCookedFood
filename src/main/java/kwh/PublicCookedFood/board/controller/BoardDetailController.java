package kwh.PublicCookedFood.board.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kwh.PublicCookedFood.board.application.query.view.BoardDetailPageView;
import kwh.PublicCookedFood.board.controller.support.BoardDetailNavigationSupport;
import kwh.PublicCookedFood.board.controller.support.BoardViewerSupport;
import kwh.PublicCookedFood.board.dto.request.BoardReportCreateRequest;
import kwh.PublicCookedFood.board.dto.request.CommentCreateRequest;
import kwh.PublicCookedFood.board.facade.BoardDetailFacade;
import kwh.PublicCookedFood.board.facade.BoardDetailQueryFacade;
import kwh.PublicCookedFood.board.facade.BoardViewer;
import kwh.PublicCookedFood.board.policy.BoardViewPolicy;
import kwh.PublicCookedFood.board.service.command.BoardReportCreateCommand;
import kwh.PublicCookedFood.board.service.comment.CommentCreateCommand;
import kwh.PublicCookedFood.board.service.comment.CommentDeleteCommand;
import kwh.PublicCookedFood.common.Paging;
import kwh.PublicCookedFood.config.oauth2.LoginAccount;
import kwh.PublicCookedFood.account.domain.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/boards")
@Hidden
public class BoardDetailController {

    private static final String BOARD_DETAIL_VIEW = "boards/boardDetail";

    private final BoardDetailQueryFacade boardDetailQueryFacade;
    private final BoardDetailFacade boardDetailFacade;
    private final BoardDetailNavigationSupport boardDetailNavigationSupport;
    private final BoardViewerSupport boardViewerSupport;
    private final BoardViewPolicy boardViewPolicy;

    @PatchMapping("/{id:[0-9]+}/comments/{commentId:[0-9]+}/delete")
    public String deleteComment(@LoginAccount Account account,
                                @PathVariable Long id,
                                @PathVariable Long commentId,
                                @RequestParam(name = "commentPage", defaultValue = "0") int commentPage,
                                @RequestParam(name = "commentSize", defaultValue = "50") int commentSize,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        BoardViewer viewer = BoardViewer.from(account);
        if (boardViewerSupport.requiresLogin(viewer)) {
            return boardViewerSupport.loginRedirect();
        }

        BoardDetailFacade.OperationResult result = boardDetailFacade.deleteComment(
                CommentDeleteCommand.of(viewer.requireAuthenticated().accountId(), id, commentId)
        );
        if (!result.success()) {
            redirectAttributes.addFlashAttribute("commentError", result.requiredMessage());
        }
        boardViewPolicy.markSkipNextDetailView(request, id);
        return boardDetailNavigationSupport.commentPageRedirect(id, commentPage, commentSize);
    }

    @PostMapping("/{id:[0-9]+}/comments")
    public String addComment(@LoginAccount Account account,
                             @PathVariable Long id,
                             @RequestParam(name = "commentPage", defaultValue = "0") int commentPage,
                             @RequestParam(name = "commentSize", defaultValue = "50") int commentSize,
                             @Valid @ModelAttribute("comment") CommentCreateRequest commentDto,
                             BindingResult bindingResult,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        BoardViewer viewer = BoardViewer.from(account);
        if (boardViewerSupport.requiresLogin(viewer)) {
            return boardViewerSupport.loginRedirect();
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("commentError", "댓글 내용을 확인해주세요.");
            boardViewPolicy.markSkipNextDetailView(request, id);
            return boardDetailNavigationSupport.commentPageRedirect(id, commentPage, commentSize);
        }

        BoardDetailFacade.OperationResult result = boardDetailFacade.addComment(
                CommentCreateCommand.of(
                        viewer.requireAuthenticated().accountId(),
                        id,
                        commentDto.getContents(),
                        commentDto.getParentId()
                ),
                commentSize
        );
        if (!result.success()) {
            redirectAttributes.addFlashAttribute("commentError", result.requiredMessage());
            boardViewPolicy.markSkipNextDetailView(request, id);
            return boardDetailNavigationSupport.commentPageRedirect(id, commentPage, commentSize);
        }
        boardViewPolicy.markSkipNextDetailView(request, id);
        return result.redirectPath()
                .map(path -> "redirect:" + path)
                .orElseGet(() -> boardDetailNavigationSupport.commentPageRedirect(id, commentPage, commentSize));
    }

    @PostMapping("/{id:[0-9]+}/scraps")
    public String addScrap(@PathVariable Long id,
                           @LoginAccount Account account,
                           @RequestParam(required = false) String redirect,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        BoardViewer viewer = BoardViewer.from(account);
        if (boardViewerSupport.requiresLogin(viewer)) {
            return boardViewerSupport.loginRedirect();
        }
        boardDetailFacade.addScrap(id, viewer.requireAuthenticated().accountId());
        if (boardDetailNavigationSupport.returnsToBoardDetail(redirect, id)) {
            boardViewPolicy.markSkipNextDetailView(request, id);
        }
        return boardDetailNavigationSupport.resolveSafeRedirect(redirect, id);
    }

    @PatchMapping("/{id:[0-9]+}/scraps/delete")
    public String deleteScrap(@PathVariable Long id,
                              @LoginAccount Account account,
                              @RequestParam(required = false) String redirect,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        BoardViewer viewer = BoardViewer.from(account);
        if (boardViewerSupport.requiresLogin(viewer)) {
            return boardViewerSupport.loginRedirect();
        }
        boardDetailFacade.removeScrap(id, viewer.requireAuthenticated().accountId());
        if (boardDetailNavigationSupport.returnsToBoardDetail(redirect, id)) {
            boardViewPolicy.markSkipNextDetailView(request, id);
        }
        return boardDetailNavigationSupport.resolveSafeRedirect(redirect, id);
    }

    @PostMapping("/{id:[0-9]+}/reports")
    public String reportBoard(@PathVariable Long id,
                              @LoginAccount Account account,
                              @RequestParam(required = false) String redirect,
                              @Valid @ModelAttribute("reportDto") BoardReportCreateRequest reportDto,
                              BindingResult bindingResult,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        BoardViewer viewer = BoardViewer.from(account);
        if (boardViewerSupport.requiresLogin(viewer)) {
            return boardViewerSupport.loginRedirect();
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("reportError", "신고 사유를 확인해주세요.");
            boardViewPolicy.markSkipNextDetailView(request, id);
            return boardDetailNavigationSupport.resolveSafeRedirect(redirect, id);
        }

        BoardDetailFacade.OperationResult result = boardDetailFacade.reportBoard(
                BoardReportCreateCommand.of(
                        id,
                        viewer.requireAuthenticated().accountId(),
                        reportDto.getReason(),
                        reportDto.getDetails()
                )
        );
        if (result.success()) {
            result.message().ifPresent(message -> redirectAttributes.addFlashAttribute("reportSuccess", message));
        } else {
            redirectAttributes.addFlashAttribute("reportError", result.requiredMessage());
        }
        boardViewPolicy.markSkipNextDetailView(request, id);
        return boardDetailNavigationSupport.resolveSafeRedirect(redirect, id);
    }

    @GetMapping("/{id:[0-9]+}")
    public String boardDetail(@LoginAccount Account account,
                              @PathVariable Long id,
                              Model model,
                              @RequestParam(name = "commentPage", defaultValue = "0") int commentPage,
                              @RequestParam(name = "commentSize", defaultValue = "50") int commentSize,
                              HttpServletRequest request) {
        Pageable commentPageable = boardDetailNavigationSupport.resolveCommentPageable(commentPage, commentSize);
        boolean increaseViews = !boardDetailNavigationSupport.isCommentPageNavigation(request, id)
                && boardViewPolicy.shouldIncreaseDetailView(request, id);
        BoardDetailPageView detailViewData = boardDetailQueryFacade.loadBoardDetail(
                id,
                account,
                commentPageable,
                increaseViews
        );

        model.addAttribute("view", detailViewData);
        Paging.addPagingAttributes(model, detailViewData.comments(), commentPageable);

        return BOARD_DETAIL_VIEW;
    }

    @PutMapping("/{id:[0-9]+}/likes")
    public String likes(@PathVariable Long id,
                        @LoginAccount Account account,
                        @RequestParam(required = false) String redirect,
                        HttpServletRequest request) {
        BoardViewer viewer = BoardViewer.from(account);
        if (boardViewerSupport.requiresLogin(viewer)) {
            return boardViewerSupport.loginRedirect();
        }

        boardDetailFacade.toggleLike(id, viewer.requireAuthenticated().accountId());
        boardViewPolicy.markSkipNextDetailView(request, id);
        return boardDetailNavigationSupport.resolveSafeRedirect(redirect, id);
    }
}
