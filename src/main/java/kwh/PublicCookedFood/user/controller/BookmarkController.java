package kwh.PublicCookedFood.user.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.constraints.Positive;
import kwh.PublicCookedFood.config.oauth2.LoginUser;
import kwh.PublicCookedFood.food.service.RecipeService;
import kwh.PublicCookedFood.user.domain.Bookmark;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.dto.request.BookmarkCreateRequest;
import kwh.PublicCookedFood.user.service.UserActivityLogService;
import kwh.PublicCookedFood.user.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/bookmarks")
@Validated
@Slf4j
@Hidden
public class BookmarkController {

    private final BookmarkService bookmarkService;

    private final RecipeService recipeService;

    private final UserActivityLogService userActivityLogService;

    @GetMapping("")
    public String bookmarkList(@LoginUser Users user,
                               @RequestParam(required = false) String search,
                               Model model){
        if (user == null) {
            return "redirect:/u/login";
        }

        List<Bookmark> userBookmarks = bookmarkService.findUserBookmarks(user, search);

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

        try {
            BookmarkCreateRequest bookmarkDto = BookmarkCreateRequest.builder()
                    .userId(user.getId())
                    .recipeID(recipeId)
                    .build();
            bookmarkService.save(bookmarkDto);
            userActivityLogService.record(
                    user.getId(),
                    "BOOKMARK_ADD",
                    "recipeId=" + recipeId
            );
        } catch (IllegalArgumentException e) {
            log.warn("북마크 저장 실패 userId={}, recipeId={}, reason={}", user.getId(), recipeId, e.getMessage());
            return "redirect:/recipes";
        }
        return "redirect:/recipes/" + recipeId;
    }

    @DeleteMapping("/{recipeId}")
    public String deleteBookmark(@LoginUser Users user, @PathVariable @Positive Long recipeId){
        if (user == null) {
            return "redirect:/u/login";
        }

        try {
            bookmarkService.delete(user, recipeService.getRecipeEntityByRecipeId(recipeId));
            userActivityLogService.record(
                    user.getId(),
                    "BOOKMARK_REMOVE",
                    "recipeId=" + recipeId
            );
        } catch (IllegalArgumentException e) {
            log.warn("북마크 삭제 실패 userId={}, recipeId={}, reason={}", user.getId(), recipeId, e.getMessage());
            return "redirect:/recipes";
        }
        return "redirect:/recipes/" + recipeId;
    }
}
