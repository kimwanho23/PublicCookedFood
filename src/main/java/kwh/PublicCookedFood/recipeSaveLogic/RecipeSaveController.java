package kwh.PublicCookedFood.recipeSaveLogic;

import io.swagger.v3.oas.annotations.tags.Tag;
import kwh.PublicCookedFood.food.dto.response.recipe_crse.Recipe_CRSE_ResponseDto;
import kwh.PublicCookedFood.food.dto.response.recipe_info.Recipe_INFO_ResponseDto;
import kwh.PublicCookedFood.food.dto.response.recipe_irdnt.Recipe_IRDNT_ResponseDto;
import kwh.PublicCookedFood.recipeSaveLogic.facade.RecipeImportFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/recipes/import")
@Tag(name = "Admin Recipe Import API")
public class RecipeSaveController {

    private final RecipeImportFacade recipeImportFacade;

    @PostMapping("/courses")
    public ResponseEntity<List<Recipe_CRSE_ResponseDto>> getRecipeCRSE() {
        return ResponseEntity.ok(recipeImportFacade.importCourses());
    }

    @PostMapping("/infos")
    public ResponseEntity<List<Recipe_INFO_ResponseDto>> getRecipeINFO() {
        return ResponseEntity.ok(recipeImportFacade.importInfos());
    }

    @PostMapping("/ingredients")
    public ResponseEntity<List<Recipe_IRDNT_ResponseDto>> getRecipeIRDNT() {
        return ResponseEntity.ok(recipeImportFacade.importIngredients());
    }
}
