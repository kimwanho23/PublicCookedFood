package kwh.PublicCookedFood.user.controller;

import kwh.PublicCookedFood.config.oauth2.LoginUser;
import kwh.PublicCookedFood.food.dto.recipe_info.Recipe_INFO_ResponseDto;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.food.service.RecipeService;
import kwh.PublicCookedFood.user.domain.Bookmark;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.dto.BookMarkDto;
import kwh.PublicCookedFood.user.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/bookmark")
@Slf4j
public class BookmarkController {

    private final BookmarkService bookmarkService;

    private final RecipeService recipeService;

    @GetMapping("/bookmarkList")
    public String bookmarkList(@LoginUser Users user, Model model){
        if (user == null) {
            return "redirect:/u/login";
        }

/*       List<BookMarkDto> userBookmarks = bookmarkService.findUserBookmarkEmail(user);
        List<Recipe_INFO_ResponseDto> recipeInfoResponseDto = userBookmarks.stream()
                .map(bookmarkDto -> recipeService.getRecipe_INFO(bookmarkDto.getRecipeID()))
                .toList();*/

        List<Bookmark> userBookmarks = bookmarkService.findUserBookmarkEmail(user);
        List<Recipe_INFO> recipeInfos = userBookmarks.stream()
                .map(Bookmark::getRecipeID) // Bookmark에서 Recipe_INFO를 바로 가져옴
                .toList();


      /*  model.addAttribute("recipeInfoResponseDto", recipeInfoResponseDto);*/

        model.addAttribute("recipeInfoResponseDto", recipeInfos);
        return "user/userBookmarks";
    }

    @PostMapping("/add/{recipeId}")
    public String addBookmark(@LoginUser Users user, @PathVariable Long recipeId){
        if (user == null) {
            return "redirect:/u/login";
        }

        String email = user.getEmail();
            BookMarkDto bookmarkDto = BookMarkDto.builder()
                    .recipeID(recipeId)
                    .email(email)
                    .build();
            bookmarkService.save(bookmarkDto);
        return "redirect:/foods/" + recipeId;
    }

    @DeleteMapping("/delete/{recipeId}")
    public String deleteBookmark(@LoginUser Users user, @PathVariable Long recipeId){
        if (user == null) {
            return "redirect:/u/login";
        }

        bookmarkService.delete(user, recipeService.getRecipeEntityByRecipeId(recipeId));
        return "redirect:/foods/" + recipeId;
    }
}
