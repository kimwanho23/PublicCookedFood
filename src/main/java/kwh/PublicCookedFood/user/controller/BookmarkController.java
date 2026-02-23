package kwh.PublicCookedFood.user.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.constraints.Positive;
import kwh.PublicCookedFood.config.oauth2.LoginUser;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.food.service.RecipeService;
import kwh.PublicCookedFood.user.domain.Bookmark;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.dto.request.BookmarkCreateRequest;
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

    @GetMapping("")
    public String bookmarkList(@LoginUser Users user, Model model){
        if (user == null) {
            return "redirect:/u/login";
        }
	
        List<Bookmark> userBookmarks = bookmarkService.findUserBookmarks(user);
        List<Recipe_INFO> recipeInfos = userBookmarks.stream()
                .map(Bookmark::getRecipeID) // Bookmark에서 Recipe_INFO를 바로 가져옴
                .toList();
	
        model.addAttribute("recipeInfoResponseDto", recipeInfos);
        return "user/userBookmarks";
    }

    @PostMapping("/{recipeId}")
    public String addBookmark(@LoginUser Users user, @PathVariable @Positive Long recipeId){
        if (user == null) {
            return "redirect:/u/login";
        }

        BookmarkCreateRequest bookmarkDto = BookmarkCreateRequest.builder()
                .userId(user.getId())
                .recipeID(recipeId)
                .build();
        bookmarkService.save(bookmarkDto);
        return "redirect:/recipes/" + recipeId;
    }

    @DeleteMapping("/{recipeId}")
    public String deleteBookmark(@LoginUser Users user, @PathVariable @Positive Long recipeId){
        if (user == null) {
            return "redirect:/u/login";
        }

        bookmarkService.delete(user, recipeService.getRecipeEntityByRecipeId(recipeId));
        return "redirect:/recipes/" + recipeId;
    }
}
