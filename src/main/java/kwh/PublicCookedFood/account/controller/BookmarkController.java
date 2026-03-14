package kwh.PublicCookedFood.account.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.constraints.Positive;
import kwh.PublicCookedFood.common.web.SafeRedirectSupport;
import kwh.PublicCookedFood.config.oauth2.LoginAccount;
import kwh.PublicCookedFood.account.domain.Bookmark;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.facade.BookmarkFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/bookmarks")
@Validated
@Hidden
public class BookmarkController {

    private final BookmarkFacade bookmarkFacade;

    @GetMapping("")
    public String bookmarkList(@LoginAccount Account account,
                               @RequestParam(required = false) String search,
                               Model model){
        if (account == null) {
            return "redirect:/u/login";
        }

        List<Bookmark> accountBookmarks = bookmarkFacade.getAccountBookmarks(account, search);

        model.addAttribute("bookmarks", accountBookmarks);
        model.addAttribute("selectedSearch", search == null ? "" : search.trim());
        return "account/accountBookmarks";
    }

    @PostMapping("/{recipeId}")
    public String addBookmark(@LoginAccount Account account,
                              @PathVariable @Positive Long recipeId,
                              @RequestParam(required = false) String redirect,
                              RedirectAttributes redirectAttributes){
        if (account == null) {
            return "redirect:/u/login";
        }

        if (!bookmarkFacade.addBookmark(account.getId(), recipeId)) {
            return "redirect:/recipes";
        }
        return resolveSuccessRedirect(recipeId, redirect, redirectAttributes);
    }

    @DeleteMapping("/{recipeId}")
    public String deleteBookmark(@LoginAccount Account account,
                                 @PathVariable @Positive Long recipeId,
                                 @RequestParam(required = false) String redirect,
                                 RedirectAttributes redirectAttributes){
        if (account == null) {
            return "redirect:/u/login";
        }

        if (!bookmarkFacade.removeBookmark(account, recipeId)) {
            return "redirect:/recipes";
        }
        return resolveSuccessRedirect(recipeId, redirect, redirectAttributes);
    }

    private void markSkipViewIncrease(RedirectAttributes redirectAttributes) {
        if (redirectAttributes == null) {
            return;
        }
        redirectAttributes.addFlashAttribute("skipViewIncrease", true);
    }

    private String resolveSuccessRedirect(Long recipeId,
                                          String redirect,
                                          RedirectAttributes redirectAttributes) {
        java.util.Optional<String> safeRedirectPath = SafeRedirectSupport.normalizeRelativePath(redirect);
        if (safeRedirectPath.isEmpty()) {
            markSkipViewIncrease(redirectAttributes);
            return SafeRedirectSupport.toRedirect("/recipes/" + recipeId);
        }
        if (isRecipeDetailPath(safeRedirectPath.get())) {
            markSkipViewIncrease(redirectAttributes);
        }
        return SafeRedirectSupport.toRedirect(safeRedirectPath.get());
    }

    private boolean isRecipeDetailPath(String path) {
        return path != null && path.matches("^/recipes/[0-9]+(?:\\?.*)?$");
    }
}
