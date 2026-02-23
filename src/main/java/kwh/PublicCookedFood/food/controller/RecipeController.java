package kwh.PublicCookedFood.food.controller;

import io.swagger.v3.oas.annotations.Hidden;
import kwh.PublicCookedFood.config.oauth2.LoginUser;
import kwh.PublicCookedFood.food.dto.response.recipe_crse.Recipe_CRSE_ResponseDto;
import kwh.PublicCookedFood.food.dto.response.recipe_info.Recipe_INFO_ResponseDto;
import kwh.PublicCookedFood.food.dto.response.recipe_irdnt.Recipe_IRDNT_ResponseDto;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;

import kwh.PublicCookedFood.food.service.RecipeService;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/recipes")
@Slf4j
@Hidden
public class RecipeController {

    private final RecipeService recipeService;

    private final BookmarkService bookmarkService;


    @GetMapping("/{id}")
    public String foodDetail(@PathVariable Long id, @LoginUser Users user, Model model){
        Recipe_INFO recipeInfo = recipeService.getRecipeEntityByRecipeId(id);
        Recipe_INFO_ResponseDto infoResponseDto = recipeService.toInfoResponse(recipeInfo);

        List<Recipe_IRDNT_ResponseDto> irdntResponseDto =
                recipeService.getRecipe_IRDNT(id);

        List<Recipe_CRSE_ResponseDto> crseResponseDto =
                recipeService.getRecipe_CRSE(id);
        boolean isLoggedIn = user != null;
        boolean isBookmarked = false;

        if (isLoggedIn) {
            // 북마크 여부 확인
            isBookmarked = bookmarkService.isBookmarked(user, recipeInfo);
            log.info(isBookmarked ? "is bookmarked" : "is unbookmarked");
        }

        List<String> categories = Arrays.asList("주재료", "부재료", "양념");
        model.addAttribute("isBookmarked", isBookmarked);
        model.addAttribute("categories", categories);
        model.addAttribute("infoResponseDto", infoResponseDto);
        model.addAttribute("irdntResponseDto", irdntResponseDto);
        model.addAttribute("crseResponseDto", crseResponseDto);
        return "foods/foodDetail";
    }

}
