package kwh.PublicCookedFood.board.contoller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kwh.PublicCookedFood.board.dto.request.BoardReportCreateRequest;
import kwh.PublicCookedFood.board.dto.request.CommentCreateRequest;
import kwh.PublicCookedFood.board.facade.BoardDetailFacade;
import kwh.PublicCookedFood.board.service.CommentNavigationService;
import kwh.PublicCookedFood.common.Paging;
import kwh.PublicCookedFood.common.web.SafeRedirectSupport;
import kwh.PublicCookedFood.config.oauth2.LoginAccount;
import kwh.PublicCookedFood.account.domain.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import java.net.URI;
import java.net.URISyntaxException;
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
                                @RequestParam(name = "commentPage", defaultValue = "0") int commentPage,
                                @RequestParam(name = "commentSize", defaultValue = "50") int commentSize,
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
        return commentPageRedirect(id, commentPage, commentSize);
    }

    @PostMapping("/{id:[0-9]+}/comments")
    public String addComment(@LoginAccount Account account,
                             @PathVariable Long id,
                             @RequestParam(name = "commentPage", defaultValue = "0") int commentPage,
                             @RequestParam(name = "commentSize", defaultValue = "50") int commentSize,
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
            return commentPageRedirect(id, commentPage, commentSize);
        }

        BoardDetailFacade.OperationResult result = boardDetailFacade.addComment(account, id, commentDto, commentSize);
        if (!result.success()) {
            redirectAttributes.addFlashAttribute("commentError", result.message());
            markSkipViewIncrease(redirectAttributes);
            return commentPageRedirect(id, commentPage, commentSize);
        }
        String successTargetPath = result.redirectPath();
        if (successTargetPath == null || successTargetPath.isBlank()) {
            markSkipViewIncrease(redirectAttributes);
            return commentPageRedirect(id, commentPage, commentSize);
        }
        markSkipViewIncrease(redirectAttributes);
        return "redirect:" + successTargetPath;
    }

    @PostMapping("/{id:[0-9]+}/scraps")
    public String addScrap(@PathVariable Long id,
                           @LoginAccount Account account,
                           @RequestParam(required = false) String redirect,
                           RedirectAttributes redirectAttributes) {
        String loginRedirect = redirectIfUnauthenticated(account);
        if (loginRedirect != null) {
            return loginRedirect;
        }
        boardDetailFacade.addScrap(id, account.getId());
        markSkipViewIncrease(redirectAttributes);
        return resolveSafeRedirectPath(redirect, id);
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
                              @RequestParam(required = false) String redirect,
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
            return resolveSafeRedirectPath(redirect, id);
        }

        BoardDetailFacade.OperationResult result = boardDetailFacade.reportBoard(id, account.getId(), reportDto);
        if (result.success()) {
            redirectAttributes.addFlashAttribute("reportSuccess", result.message());
        } else {
            redirectAttributes.addFlashAttribute("reportError", result.message());
        }
        markSkipViewIncrease(redirectAttributes);
        return resolveSafeRedirectPath(redirect, id);
    }

    @GetMapping("/{id:[0-9]+}")
    public String boardDetail(@LoginAccount Account account,
                              @PathVariable Long id,
                              Model model,
                              @RequestParam(name = "commentPage", defaultValue = "0") int commentPage,
                              @RequestParam(name = "commentSize", defaultValue = "50") int commentSize,
                              HttpServletRequest request) {
        Pageable commentPageable = resolveCommentPageable(commentPage, commentSize);
        boolean increaseViews = shouldIncreaseViews(request, id);
        BoardDetailFacade.BoardDetailViewData detailViewData = boardDetailFacade.loadBoardDetail(
                id,
                account,
                commentPageable,
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
        Paging.addPagingAttributes(model, detailViewData.comments(), commentPageable);

        return BOARD_DETAIL_VIEW;
    }

    @PutMapping("/{id:[0-9]+}/likes")
    public String likes(@PathVariable Long id,
                        @LoginAccount Account account,
                        @RequestParam(required = false) String redirect,
                        RedirectAttributes redirectAttributes) {
        String loginRedirect = redirectIfUnauthenticated(account);
        if (loginRedirect != null) {
            return loginRedirect;
        }

        boardDetailFacade.toggleLike(id, account.getId());
        markSkipViewIncrease(redirectAttributes);
        return resolveSafeRedirectPath(redirect, id);
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

    private String commentPageRedirect(Long boardId, int commentPage, int commentSize) {
        String targetPath = buildCommentPagePath(boardId, commentPage, commentSize, "#board-comments");
        return "redirect:" + targetPath;
    }

    private String buildCommentPagePath(Long boardId, int commentPage, int commentSize, String anchor) {
        Pageable pageable = resolveCommentPageable(commentPage, commentSize);
        String suffix = anchor == null ? "" : anchor;
        return "/boards/" + boardId
                + "?commentPage=" + pageable.getPageNumber()
                + "&commentSize=" + pageable.getPageSize()
                + suffix;
    }

    private void markSkipViewIncrease(RedirectAttributes redirectAttributes) {
        if (redirectAttributes == null) {
            return;
        }
        redirectAttributes.addFlashAttribute("skipViewIncrease", true);
    }

    private boolean shouldIncreaseViews(HttpServletRequest request, Long boardId) {
        if (request == null) {
            return true;
        }
        Map<String, ?> flashMap = RequestContextUtils.getInputFlashMap(request);
        Object skipViewIncrease = flashMap == null ? null : flashMap.get("skipViewIncrease");
        if (Boolean.TRUE.equals(skipViewIncrease)) {
            return false;
        }
        return !isCommentPageNavigation(request, boardId);
    }

    private boolean isCommentPageNavigation(HttpServletRequest request, Long boardId) {
        if (request == null || boardId == null) {
            return false;
        }
        if (request.getParameter("commentPage") == null && request.getParameter("commentSize") == null) {
            return false;
        }

        URI refererUri = parseUri(request.getHeader("Referer"));
        if (refererUri == null) {
            return false;
        }
        String expectedPath = "/boards/" + boardId;
        if (!expectedPath.equals(refererUri.getPath()) || !expectedPath.equals(request.getRequestURI())) {
            return false;
        }
        String refererHost = refererUri.getHost();
        if (refererHost != null && !refererHost.equalsIgnoreCase(request.getServerName())) {
            return false;
        }

        String currentCommentPage = normalizeQueryValue(request.getParameter("commentPage"));
        String currentCommentSize = normalizeQueryValue(request.getParameter("commentSize"));
        String previousCommentPage = normalizeQueryValue(extractQueryParam(refererUri, "commentPage"));
        String previousCommentSize = normalizeQueryValue(extractQueryParam(refererUri, "commentSize"));
        return !currentCommentPage.equals(previousCommentPage)
                || !currentCommentSize.equals(previousCommentSize);
    }

    private URI parseUri(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new URI(value);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private String extractQueryParam(URI uri, String key) {
        if (uri == null || key == null || key.isBlank()) {
            return "";
        }
        String query = uri.getQuery();
        if (query == null || query.isBlank()) {
            return "";
        }
        String prefix = key + "=";
        for (String token : query.split("&")) {
            if (token.startsWith(prefix)) {
                return token.substring(prefix.length());
            }
        }
        return "";
    }

    private String normalizeQueryValue(String value) {
        return value == null ? "" : value.trim();
    }

    private Pageable resolveCommentPageable(int commentPage, int commentSize) {
        int normalizedPage = Math.max(commentPage, 0);
        int normalizedSize = Math.min(
                Math.max(commentSize, 1),
                CommentNavigationService.MAX_COMMENT_PAGE_SIZE
        );
        return PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Order.asc("regTime")));
    }
}
