package kwh.PublicCookedFood.food.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kwh.PublicCookedFood.common.Paging;
import kwh.PublicCookedFood.common.dto.request.RecipeSearchQuery;
import kwh.PublicCookedFood.food.dto.response.RecipeCategoryGroupResponse;
import kwh.PublicCookedFood.food.dto.response.recipe_info.Recipe_INFO_ResponseDto;
import kwh.PublicCookedFood.food.service.RecipeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
@Hidden
public class MainController {

    private final RecipeService recipeService;

    @GetMapping("/")
    public String root() {
        return "redirect:/recipes";
    }

    @GetMapping("/main")
    public String Home() {
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
            String canonicalRedirect = buildCanonicalRecipeListRedirect(request);
            if (canonicalRedirect != null) {
                return canonicalRedirect;
            }
        }

        String type = query.normalizedType();
        String nation = query.normalizedNation();
        String ingredient = query.normalizedIngredient();
        String keyword = query.normalizedKeyword();
        String search = query.normalizedSearch();

        if (bindingResult.hasErrors()) {
            type = null;
            nation = null;
            ingredient = null;
            keyword = null;
            search = null;
            model.addAttribute("queryErrorMsg", "검색 조건이 유효하지 않아 기본 목록을 표시합니다.");
        }

        Page<Recipe_INFO_ResponseDto> infoResponseDto = getRecipeInfo(pageable, type, nation, ingredient, keyword, search); //레시피 필터링
        Paging.addPagingAttributes(model, infoResponseDto, pageable); //페이징 알고리즘
        model.addAttribute("infoResponseDto", infoResponseDto);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedNation", nation);
        model.addAttribute("selectedIngredient", ingredient);
        model.addAttribute("selectedKeyword", keyword);
        model.addAttribute("selectedSearch", search);

        if (!ajaxRequest) {
            List<RecipeCategoryGroupResponse> categories = createCategories(); //카테고리 목록
            model.addAttribute("categories", categories);
        }

        if (ajaxRequest) {
            return "foods/index :: recipeResults";
        }
        return "/foods/index";
    }

    private Page<Recipe_INFO_ResponseDto> getRecipeInfo(Pageable pageable,
                                                        String type,
                                                        String nation,
                                                        String ingredient,
                                                        String keyword,
                                                        String search) {
        if ((type == null || type.isEmpty())
                && (nation == null || nation.isEmpty())
                && (ingredient == null || ingredient.isEmpty())
                && (keyword == null || keyword.isEmpty())
                && (search == null || search.isEmpty())) {
            return recipeService.getAllRecipe_INFO(pageable);
        }
        return recipeService.getFilteredRecipeList(type, nation, ingredient, keyword, search, pageable);
    }

    private List<RecipeCategoryGroupResponse> createCategories() {
        List<String> allTyNmList = recipeService.getRecipeTy_NM();
        List<String> allNationNmList = recipeService.getRecipeNation_NM();
        List<String> allIrdntCodeList = recipeService.getRecipeIrdntCODE();

        return List.of(
                new RecipeCategoryGroupResponse("음식별", "nation", allNationNmList),
                new RecipeCategoryGroupResponse("재료별", "ingredient", allIrdntCodeList),
                new RecipeCategoryGroupResponse("분류별", "type", allTyNmList)
        );
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }

    private String buildCanonicalRecipeListRedirect(HttpServletRequest request) {
        boolean hasEmptyParam = request.getParameterMap().values().stream()
                .flatMap(Arrays::stream)
                .anyMatch(value -> value == null || value.trim().isEmpty());
        if (!hasEmptyParam) {
            return null;
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/recipes");
        request.getParameterMap().forEach((key, values) -> {
            if (values == null) {
                return;
            }
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    builder.queryParam(key, value.trim());
                }
            }
        });
        return "redirect:" + builder.build().encode().toUriString();
    }
}
