package kwh.PublicCookedFood.food.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kwh.PublicCookedFood.common.Paging;
import kwh.PublicCookedFood.common.dto.request.RecipeSearchQuery;
import kwh.PublicCookedFood.common.web.QueryParamCanonicalizer;
import kwh.PublicCookedFood.food.facade.RecipeMainFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
@RequiredArgsConstructor
@Hidden
public class MainController {

    private final RecipeMainFacade recipeMainFacade;

    @GetMapping("/")
    public String root() {
        return "redirect:/recipes";
    }

    @GetMapping("/main")
    public String home(Model model) {
        RecipeMainFacade.HomeViewData data = recipeMainFacade.loadHomeData();
        model.addAttribute("popularBoards", data.popularBoards());
        model.addAttribute("recipeRankings", data.recipeRankings());
        return "/foods/main";
    }

    @GetMapping("/recipes")
    public String index(@PageableDefault(size = 15, sort = "rowNUM", direction = Sort.Direction.ASC) Pageable pageable,
                        @Valid @ModelAttribute("query") RecipeSearchQuery query,
                        BindingResult bindingResult,
                        HttpServletRequest request,
                        Model model){
        boolean ajaxRequest = isAjaxRequest(request);
        if (!ajaxRequest) {
            String canonicalRedirect = QueryParamCanonicalizer.buildRedirectIfHasEmptyValues(request, "/recipes");
            if (canonicalRedirect != null) {
                return canonicalRedirect;
            }
        }

        RecipeMainFacade.RecipeListViewData viewData =
                recipeMainFacade.loadRecipeList(pageable, query, bindingResult.hasErrors());

        if (viewData.queryErrorMsg() != null) {
            model.addAttribute("queryErrorMsg", viewData.queryErrorMsg());
        }

        Paging.addPagingAttributes(model, viewData.recipePage(), pageable);
        model.addAttribute("infoResponseDto", viewData.recipePage());
        model.addAttribute("selectedTypes", viewData.selectedTypes());
        model.addAttribute("selectedNations", viewData.selectedNations());
        model.addAttribute("selectedIngredients", viewData.selectedIngredients());
        model.addAttribute("selectedKeyword", viewData.selectedKeyword());
        model.addAttribute("selectedSearch", viewData.selectedSearch());
        model.addAttribute("categories", viewData.categories());

        return "/foods/index";
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }
}
