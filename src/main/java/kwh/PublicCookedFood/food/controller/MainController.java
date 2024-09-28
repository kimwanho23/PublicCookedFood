package kwh.PublicCookedFood.food.controller;

import jakarta.servlet.http.HttpServletRequest;
import kwh.PublicCookedFood.common.Paging;
import kwh.PublicCookedFood.food.dto.recipe_info.Recipe_INFO_ResponseDto;
import kwh.PublicCookedFood.food.service.RecipeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MainController {

    private final RecipeService recipeService;

    @GetMapping(value = "/foods")
    public String index(@PageableDefault(size = 15) Pageable pageable,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) String search,
                        Model model,HttpServletRequest request){

        if ((keyword != null && keyword.trim().isEmpty()) || (search != null && search.trim().isEmpty())) {
            return Paging.handleEmptyParamsRedirect(request, keyword, search);
        }

        Page<Recipe_INFO_ResponseDto> infoResponseDto = getRecipeInfo(pageable, keyword, search); //레시피 필터링
        Paging.addPagingAttributes(model, infoResponseDto, pageable); //페이징 알고리즘
        List<Map<String, Object>> categories = createCategories(); //카테고리 목록

        model.addAttribute("categories", categories);
        model.addAttribute("infoResponseDto", infoResponseDto);
        model.addAttribute("keyword", keyword);
        model.addAttribute("search", search);

        return "/foods/index";
    }


    private Page<Recipe_INFO_ResponseDto> getRecipeInfo(Pageable pageable, String keyword, String search) {
        if ((keyword == null || keyword.isEmpty()) && (search == null || search.isEmpty())) {
            return recipeService.getAllRecipe_INFO(pageable);
        }else {
            return recipeService.getFilteredRecipeList(keyword, search, pageable);
        }
    }

    private List<Map<String, Object>> createCategories() {
        List<String> allTyNmList = recipeService.getRecipeTy_NM();
        List<String> allNationNmList = recipeService.getRecipeNation_NM();
        List<String> allIrdntCodeList = recipeService.getRecipeIrdntCODE();

        return List.of(
                Map.of("title", "음식별", "items", allNationNmList),
                Map.of("title", "재료별", "items", allIrdntCodeList),
                Map.of("title", "분류별", "items", allTyNmList)
        );
    }
}
