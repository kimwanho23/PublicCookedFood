package kwh.PublicCookedFood.food.controller;

import jakarta.servlet.http.HttpSession;
import kwh.PublicCookedFood.food.dto.recipe_info.Recipe_INFO_ResponseDto;
import kwh.PublicCookedFood.food.service.RecipeService;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.dto.UserSaveDto;
import kwh.PublicCookedFood.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MainController {

    private final RecipeService recipeService;

    @GetMapping(value = "/foods")
    public String index(@PageableDefault(size = 15) Pageable pageable,
                        @RequestParam(required = false) List<String> tyNmList,
                        @RequestParam(required = false) List<String> nationNmList,
                        @RequestParam(required = false) List<String> irdntCodeList,
                        Model model){

        Page<Recipe_INFO_ResponseDto> infoResponseDto = getRecipeInfo(pageable, tyNmList, nationNmList, irdntCodeList);
        addPagingAttributes(model, infoResponseDto, pageable);

        List<String> allTyNmList = recipeService.getRecipeTy_NM();
        List<String> allNationNmList = recipeService.getRecipeNation_NM();
        List<String> allIrdntCodeList = recipeService.getRecipeIrdntCODE();

        model.addAttribute("infoResponseDto", infoResponseDto);
        model.addAttribute("allNationNmList", allNationNmList);
        model.addAttribute("allIrdntCodeList", allIrdntCodeList);
        model.addAttribute("allTyNmList", allTyNmList);

        return "foods/index";
    }

    private Page<Recipe_INFO_ResponseDto> getRecipeInfo(Pageable pageable,
                                                        List<String> tyNmList,
                                                        List<String> nationNmList,
                                                        List<String> irdntCodeList) {
        if ((tyNmList == null || tyNmList.isEmpty()) &&
                (nationNmList == null || nationNmList.isEmpty()) &&
                (irdntCodeList == null || irdntCodeList.isEmpty())) {
            return recipeService.getAllRecipe_INFO(pageable);
        } else {
            return recipeService.getFilteredRecipeList(tyNmList, nationNmList, irdntCodeList, pageable);
        }
    }

    private void addPagingAttributes(Model model, Page<Recipe_INFO_ResponseDto> infoResponseDto, Pageable pageable) {
        int totalPages = infoResponseDto.getTotalPages();
        int currentPage = pageable.getPageNumber();

        int currentGroup = currentPage / 10;
        int startPage = currentGroup * 10;
        int endPage = Math.min(startPage + 9, totalPages - 1);

        boolean hasPreviousGroup = startPage > 0;
        boolean hasNextGroup = endPage < totalPages - 1;

        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("hasPreviousGroup", hasPreviousGroup);
        model.addAttribute("hasNextGroup", hasNextGroup);
    }

    @GetMapping("header")
    public String header(){
        return "fragments/header";
    }

    @GetMapping("footer")
    public String footer(){
        return "fragments/footer";
    }



}
