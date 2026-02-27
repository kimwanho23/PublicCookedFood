package kwh.PublicCookedFood.user.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.constraints.Positive;
import kwh.PublicCookedFood.config.oauth2.LoginUser;
import kwh.PublicCookedFood.user.domain.Bookmark;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.facade.BookmarkFacade;
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

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/bookmarks")
@Validated
@Hidden
public class BookmarkController {

    private final BookmarkFacade bookmarkFacade;

    @GetMapping("")
    public String bookmarkList(@LoginUser Users user,
                               @RequestParam(required = false) String search,
                               Model model){
        if (user == null) {
            return "redirect:/u/login";
        }

        List<Bookmark> userBookmarks = bookmarkFacade.getUserBookmarks(user, search);

        model.addAttribute("bookmarks", userBookmarks);
        model.addAttribute("selectedSearch", search == null ? "" : search.trim());
        return "user/userBookmarks";
    }

    @PostMapping("/{recipeId}")
    public String addBookmark(@LoginUser Users user,
                              @PathVariable @Positive Long recipeId){
        if (user == null) {
            return "redirect:/u/login";
        }

        if (!bookmarkFacade.addBookmark(user.getId(), recipeId)) {
            return "redirect:/recipes";
        }
        return "redirect:/recipes/" + recipeId;
    }

    @DeleteMapping("/{recipeId}")
    public String deleteBookmark(@LoginUser Users user, @PathVariable @Positive Long recipeId){
        if (user == null) {
            return "redirect:/u/login";
        }

        if (!bookmarkFacade.removeBookmark(user, recipeId)) {
            return "redirect:/recipes";
        }
        return "redirect:/recipes/" + recipeId;
    }
}
