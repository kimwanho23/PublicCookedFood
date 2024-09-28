package kwh.PublicCookedFood.user.controller;

import jakarta.servlet.http.HttpSession;
import kwh.PublicCookedFood.food.dto.recipe_info.Recipe_INFO_ResponseDto;
import kwh.PublicCookedFood.food.service.RecipeService;
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
    public String bookmarkList(HttpSession session, Model model){
        Users user = (Users) session.getAttribute("user");
        List<BookMarkDto> userBookmarks = bookmarkService.findUserBookmarks(user);
       List<Recipe_INFO_ResponseDto> recipeInfoResponseDto = userBookmarks.stream()
                .map(bookmarkDto -> recipeService.getRecipe_INFO(bookmarkDto.getRecipeID()))
                .toList();


        model.addAttribute("recipeInfoResponseDto", recipeInfoResponseDto);
        return "user/userBookmarks";
    }

    @GetMapping("/add/{recipeId}")
    public String addBookmark(@PathVariable Long recipeId, HttpSession session){
        Users user = (Users) session.getAttribute("user");
            BookMarkDto bookmarkDto = BookMarkDto.builder()
                    .recipeID(recipeId)
                    .email(user.getEmail())
                    .build();
            bookmarkService.save(bookmarkDto);
        return "redirect:/foods/" + recipeId;
    }

    @GetMapping("/delete/{recipeId}")
    public String deleteBookmark(@PathVariable Long recipeId, HttpSession session){
        Users user = (Users) session.getAttribute("user");
        bookmarkService.delete(user, recipeService.getRecipe_INFO(recipeId).toEntity());
        return "redirect:/foods/" + recipeId;
    }
}
