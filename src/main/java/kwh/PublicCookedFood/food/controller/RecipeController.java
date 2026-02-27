package kwh.PublicCookedFood.food.controller;

import io.swagger.v3.oas.annotations.Hidden;
import kwh.PublicCookedFood.config.oauth2.LoginUser;
import kwh.PublicCookedFood.food.facade.RecipeDetailFacade;
import kwh.PublicCookedFood.user.domain.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/recipes")
@Hidden
public class RecipeController {

    private final RecipeDetailFacade recipeDetailFacade;

    @GetMapping("/{id}")
    public String foodDetail(@PathVariable Long id, @LoginUser Users user, Model model){
        RecipeDetailFacade.RecipeDetailViewData viewData = recipeDetailFacade.loadRecipeDetail(id, user);

        model.addAttribute("isBookmarked", viewData.bookmarked());
        model.addAttribute("categories", viewData.categories());
        model.addAttribute("infoResponseDto", viewData.infoResponseDto());
        model.addAttribute("irdntResponseDto", viewData.irdntResponseDto());
        model.addAttribute("crseResponseDto", viewData.crseResponseDto());
        model.addAttribute("reviewSummary", viewData.reviewSummary());
        model.addAttribute("reviews", viewData.reviews());
        model.addAttribute("myReview", viewData.myReview());
        return "foods/foodDetail";
    }

    @PostMapping("/{id}/reviews")
    public String upsertReview(@PathVariable Long id,
                               @LoginUser Users user,
                               @RequestParam Integer rating,
                               @RequestParam(required = false) String contents,
                               RedirectAttributes redirectAttributes) {
        if (user == null) {
            return "redirect:/u/login";
        }

        RecipeDetailFacade.ReviewUpsertResult result =
                recipeDetailFacade.upsertReview(id, user.getId(), rating, contents);
        if (result.success()) {
            redirectAttributes.addFlashAttribute("reviewMessage", result.message());
        } else {
            redirectAttributes.addFlashAttribute("reviewErrorMessage", result.message());
        }
        return "redirect:/recipes/" + id;
    }
}
