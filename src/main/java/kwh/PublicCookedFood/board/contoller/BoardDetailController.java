package kwh.PublicCookedFood.board.contoller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import kwh.PublicCookedFood.board.dto.request.BoardReportCreateRequest;
import kwh.PublicCookedFood.board.dto.request.CommentCreateRequest;
import kwh.PublicCookedFood.board.facade.BoardDetailFacade;
import kwh.PublicCookedFood.config.oauth2.LoginUser;
import kwh.PublicCookedFood.user.domain.Users;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/boards")
@Hidden
public class BoardDetailController {

    private static final String LOGIN_REDIRECT = "redirect:/u/login";
    private static final String BOARD_DETAIL_VIEW = "/boards/boardDetail";

    private final BoardDetailFacade boardDetailFacade;

    @PatchMapping("/{id:[0-9]+}/comments/{commentId:[0-9]+}/delete")
    public String deleteComment(@LoginUser Users user, @PathVariable Long id, @PathVariable Long commentId) {
        String loginRedirect = redirectIfUnauthenticated(user);
        if (loginRedirect != null) {
            return loginRedirect;
        }

        boardDetailFacade.deleteComment(user, id, commentId);
        return boardDetailRedirect(id);
    }

    @PostMapping("/{id:[0-9]+}/comments")
    public String addComment(@LoginUser Users user,
                             @PathVariable Long id,
                             @Valid @ModelAttribute("comment") CommentCreateRequest commentDto,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        String loginRedirect = redirectIfUnauthenticated(user);
        if (loginRedirect != null) {
            return loginRedirect;
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("commentError", "댓글 내용을 확인해주세요.");
            return boardDetailRedirect(id);
        }

        BoardDetailFacade.OperationResult result = boardDetailFacade.addComment(user, id, commentDto);
        if (!result.success()) {
            redirectAttributes.addFlashAttribute("commentError", result.message());
        }
        return boardDetailRedirect(id);
    }

    @PostMapping("/{id:[0-9]+}/scraps")
    public String addScrap(@PathVariable Long id, @LoginUser Users user) {
        String loginRedirect = redirectIfUnauthenticated(user);
        if (loginRedirect != null) {
            return loginRedirect;
        }
        boardDetailFacade.addScrap(id, user.getId());
        return boardDetailRedirect(id);
    }

    @PatchMapping("/{id:[0-9]+}/scraps/delete")
    public String deleteScrap(@PathVariable Long id, @LoginUser Users user) {
        String loginRedirect = redirectIfUnauthenticated(user);
        if (loginRedirect != null) {
            return loginRedirect;
        }
        boardDetailFacade.removeScrap(id, user.getId());
        return boardDetailRedirect(id);
    }

    @PostMapping("/{id:[0-9]+}/reports")
    public String reportBoard(@PathVariable Long id,
                              @LoginUser Users user,
                              @Valid @ModelAttribute("reportDto") BoardReportCreateRequest reportDto,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        String loginRedirect = redirectIfUnauthenticated(user);
        if (loginRedirect != null) {
            return loginRedirect;
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("reportError", "신고 사유를 확인해주세요.");
            return boardDetailRedirect(id);
        }

        BoardDetailFacade.OperationResult result = boardDetailFacade.reportBoard(id, user.getId(), reportDto);
        if (result.success()) {
            redirectAttributes.addFlashAttribute("reportSuccess", result.message());
        } else {
            redirectAttributes.addFlashAttribute("reportError", result.message());
        }
        return boardDetailRedirect(id);
    }

    @GetMapping("/{id:[0-9]+}")
    public String boardDetail(@LoginUser Users user,
                              @PathVariable Long id,
                              Model model,
                              @PageableDefault(page = 0, size = 50, sort = "regTime", direction = Sort.Direction.ASC) Pageable pageable) {
        BoardDetailFacade.BoardDetailViewData detailViewData = boardDetailFacade.loadBoardDetail(id, user, pageable);

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
        model.addAttribute("currentUserId", detailViewData.currentUserId());
        model.addAttribute("comments", detailViewData.comments());
        model.addAttribute("commentsCount", detailViewData.commentsCount());

        return BOARD_DETAIL_VIEW;
    }

    @PutMapping("/{id:[0-9]+}/likes")
    public String likes(@PathVariable Long id, @LoginUser Users user) {
        String loginRedirect = redirectIfUnauthenticated(user);
        if (loginRedirect != null) {
            return loginRedirect;
        }

        boardDetailFacade.toggleLike(id, user.getId());
        return boardDetailRedirect(id);
    }

    private String redirectIfUnauthenticated(Users user) {
        return user == null ? LOGIN_REDIRECT : null;
    }

    private String boardDetailRedirect(Long boardId) {
        return "redirect:/boards/" + boardId;
    }
}
