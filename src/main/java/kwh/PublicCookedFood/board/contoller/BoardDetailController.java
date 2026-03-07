package kwh.PublicCookedFood.board.contoller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kwh.PublicCookedFood.board.dto.request.BoardReportCreateRequest;
import kwh.PublicCookedFood.board.dto.request.CommentCreateRequest;
import kwh.PublicCookedFood.board.facade.BoardDetailFacade;
import kwh.PublicCookedFood.common.web.SafeRedirectSupport;
import kwh.PublicCookedFood.config.oauth2.LoginAccount;
import kwh.PublicCookedFood.account.domain.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/boards")
@Hidden
public class BoardDetailController {

    private static final String LOGIN_REDIRECT = "redirect:/u/login";
    private static final String BOARD_DETAIL_VIEW = "/boards/boardDetail";

    private final BoardDetailFacade boardDetailFacade;

    @PatchMapping("/{id:[0-9]+}/comments/{commentId:[0-9]+}/delete")
    public String deleteComment(@LoginAccount Account account,
                                @PathVariable Long id,
                                @PathVariable Long commentId,
                                RedirectAttributes redirectAttributes) {
        String loginRedirect = redirectIfUnauthenticated(account);
        if (loginRedirect != null) {
            return loginRedirect;
        }

        BoardDetailFacade.OperationResult result = boardDetailFacade.deleteComment(account, id, commentId);
        if (!result.success()) {
            redirectAttributes.addFlashAttribute("commentError", result.message());
        }
        markSkipViewIncrease(redirectAttributes);
        return boardDetailRedirect(id);
    }

    @PostMapping("/{id:[0-9]+}/comments")
    public String addComment(@LoginAccount Account account,
                             @PathVariable Long id,
                             @Valid @ModelAttribute("comment") CommentCreateRequest commentDto,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        String loginRedirect = redirectIfUnauthenticated(account);
        if (loginRedirect != null) {
            return loginRedirect;
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("commentError", "댓글 내용을 확인해주세요.");
            markSkipViewIncrease(redirectAttributes);
            return boardDetailRedirect(id);
        }

        BoardDetailFacade.OperationResult result = boardDetailFacade.addComment(account, id, commentDto);
        if (!result.success()) {
            redirectAttributes.addFlashAttribute("commentError", result.message());
        }
        markSkipViewIncrease(redirectAttributes);
        return boardDetailRedirect(id);
    }

    @PostMapping("/{id:[0-9]+}/scraps")
    public String addScrap(@PathVariable Long id,
                           @LoginAccount Account account,
                           RedirectAttributes redirectAttributes) {
        String loginRedirect = redirectIfUnauthenticated(account);
        if (loginRedirect != null) {
            return loginRedirect;
        }
        boardDetailFacade.addScrap(id, account.getId());
        markSkipViewIncrease(redirectAttributes);
        return boardDetailRedirect(id);
    }

    @PatchMapping("/{id:[0-9]+}/scraps/delete")
    public String deleteScrap(@PathVariable Long id,
                              @LoginAccount Account account,
                              @RequestParam(required = false) String redirect,
                              RedirectAttributes redirectAttributes) {
        String loginRedirect = redirectIfUnauthenticated(account);
        if (loginRedirect != null) {
            return loginRedirect;
        }
        boardDetailFacade.removeScrap(id, account.getId());
        markSkipViewIncrease(redirectAttributes);
        return resolveSafeRedirectPath(redirect, id);
    }

    @PostMapping("/{id:[0-9]+}/reports")
    public String reportBoard(@PathVariable Long id,
                              @LoginAccount Account account,
                              @Valid @ModelAttribute("reportDto") BoardReportCreateRequest reportDto,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        String loginRedirect = redirectIfUnauthenticated(account);
        if (loginRedirect != null) {
            return loginRedirect;
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("reportError", "신고 사유를 확인해주세요.");
            markSkipViewIncrease(redirectAttributes);
            return boardDetailRedirect(id);
        }

        BoardDetailFacade.OperationResult result = boardDetailFacade.reportBoard(id, account.getId(), reportDto);
        if (result.success()) {
            redirectAttributes.addFlashAttribute("reportSuccess", result.message());
        } else {
            redirectAttributes.addFlashAttribute("reportError", result.message());
        }
        markSkipViewIncrease(redirectAttributes);
        return boardDetailRedirect(id);
    }

    @GetMapping("/{id:[0-9]+}")
    public String boardDetail(@LoginAccount Account account,
                              @PathVariable Long id,
                              Model model,
                              @PageableDefault(page = 0, size = 50, sort = "regTime", direction = Sort.Direction.ASC) Pageable pageable,
                              HttpServletRequest request) {
        boolean increaseViews = shouldIncreaseViews(request);
        BoardDetailFacade.BoardDetailViewData detailViewData = boardDetailFacade.loadBoardDetail(
                id,
                account,
                pageable,
                increaseViews
        );

        model.addAttribute("id", id);
        model.addAttribute("boardDto", detailViewData.boardDto());
        model.addAttribute("likes", detailViewData.likes());
        model.addAttribute("scraps", detailViewData.scraps());
        if (detailViewData.myLike() != null) {
            model.addAttribute("myLike", detailViewData.myLike());
        }
        model.addAttribute("myScrap", detailViewData.myScrap());
        model.addAttribute("myReport", detailViewData.myReport());
        model.addAttribute("myBlockedAuthor", detailViewData.myBlockedAuthor());
        model.addAttribute("boardInteractionBlocked", detailViewData.boardInteractionBlocked());
        model.addAttribute("reportReasons", detailViewData.reportReasons());
        model.addAttribute("currentAccountId", detailViewData.currentAccountId());
        model.addAttribute("comments", detailViewData.comments());
        model.addAttribute("commentsCount", detailViewData.commentsCount());

        return BOARD_DETAIL_VIEW;
    }

    @PutMapping("/{id:[0-9]+}/likes")
    public String likes(@PathVariable Long id,
                        @LoginAccount Account account,
                        RedirectAttributes redirectAttributes) {
        String loginRedirect = redirectIfUnauthenticated(account);
        if (loginRedirect != null) {
            return loginRedirect;
        }

        boardDetailFacade.toggleLike(id, account.getId());
        markSkipViewIncrease(redirectAttributes);
        return boardDetailRedirect(id);
    }

    private String redirectIfUnauthenticated(Account account) {
        return account == null ? LOGIN_REDIRECT : null;
    }

    private String boardDetailRedirect(Long boardId) {
        return "redirect:/boards/" + boardId;
    }

    private String resolveSafeRedirectPath(String redirect, Long boardId) {
        return SafeRedirectSupport.toRedirectOrDefault(redirect, "/boards/" + boardId);
    }

    private void markSkipViewIncrease(RedirectAttributes redirectAttributes) {
        if (redirectAttributes == null) {
            return;
        }
        redirectAttributes.addFlashAttribute("skipViewIncrease", true);
    }

    private boolean shouldIncreaseViews(HttpServletRequest request) {
        if (request == null) {
            return true;
        }
        Map<String, ?> flashMap = RequestContextUtils.getInputFlashMap(request);
        Object skipViewIncrease = flashMap == null ? null : flashMap.get("skipViewIncrease");
        return !Boolean.TRUE.equals(skipViewIncrease);
    }
}
