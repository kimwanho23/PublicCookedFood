package kwh.PublicCookedFood.food.controller;


import kwh.PublicCookedFood.common.GlobalControllerAdvice;
import kwh.PublicCookedFood.food.dto.recipe_crse.Recipe_CRSE_ResponseDto;
import kwh.PublicCookedFood.food.dto.recipe_info.Recipe_INFO_ResponseDto;
import kwh.PublicCookedFood.food.dto.recipe_irdnt.Recipe_IRDNT_ResponseDto;

import kwh.PublicCookedFood.food.service.RecipeService;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.service.BookmarkService;
import kwh.PublicCookedFood.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/foods")
@Slf4j
public class RecipeController {

    private final RecipeService recipeService;

    private final BookmarkService bookmarkService;


    @GetMapping("/{id}")
    public String foodDetail(@PathVariable Long id, Model model){
        Recipe_INFO_ResponseDto infoResponseDto =
                recipeService.getRecipe_INFO(id);

        List<Recipe_IRDNT_ResponseDto> irdntResponseDto =
                recipeService.getRecipe_IRDNT(id);

        List<Recipe_CRSE_ResponseDto> crseResponseDto =
                recipeService.getRecipe_CRSE(id);

        Users user = (Users) model.getAttribute("user");
        boolean isLoggedIn = user != null;

        if (isLoggedIn) {
            // 북마크 여부 확인
            boolean isBookmarked = bookmarkService.isBookmarked(user, infoResponseDto.toEntity());
            model.addAttribute("isBookmarked", isBookmarked);
            log.info(isBookmarked ? "is bookmarked" : "is unbookmarked");
        }


        model.addAttribute("infoResponseDto", infoResponseDto);
        model.addAttribute("irdntResponseDto", irdntResponseDto);
        model.addAttribute("crseResponseDto", crseResponseDto);
        return "foods/foodDetail";
    }

}
