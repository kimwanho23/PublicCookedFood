package kwh.PublicCookedFood.food.service;

import kwh.PublicCookedFood.food.dto.response.recipe_crse.Recipe_CRSE_ResponseDto;
import kwh.PublicCookedFood.food.dto.response.recipe_info.Recipe_INFO_ResponseDto;
import kwh.PublicCookedFood.food.dto.response.recipe_irdnt.Recipe_IRDNT_ResponseDto;
import kwh.PublicCookedFood.food.entity.Recipe_CRSE;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.food.entity.Recipe_IRDNT;
import kwh.PublicCookedFood.food.repository.Recipe_CRSE_Repository;
import kwh.PublicCookedFood.food.repository.Recipe_INFO_Repository;
import kwh.PublicCookedFood.food.repository.Recipe_IRDNT_Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;


@Service
@RequiredArgsConstructor
public class RecipeService {
    private final Recipe_CRSE_Repository crseRepository;

    private final Recipe_INFO_Repository infoRepository;

    private final Recipe_IRDNT_Repository irdntRepository;

    public Recipe_INFO getRecipeEntityByRecipeId(Long recipeId) {
        return infoRepository.findByRecipeID(recipeId)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found"));
    }

    public List<Recipe_CRSE_ResponseDto> getRecipeCrse(Long id){
        return crseRepository.findAllByRecipeIDOrderByCookingNOAsc(id)
                .stream()
                .map(this::toCrseResponse)
                .toList(); //레시피 과정정보
    }

    public List<Recipe_IRDNT_ResponseDto> getRecipeIrdnt(Long recipeId){
        return irdntRepository.findAllByRecipeIDOrderByIrdntTYCODEAsc(recipeId)
                .stream()
                .map(this::toIrdntResponse)
                .toList(); //레시피 재료정보
    }

    /////////////////   카테고리명   ////////////////////////
    @Cacheable("recipeNationCategories")
    public List<String> getRecipeNationNames(){
        return normalizeCategoryValues(infoRepository.findDistinctNationNM());
    }

    @Cacheable("recipeTypeCategories")
    public List<String> getRecipeTypeNames(){
        return normalizeCategoryValues(infoRepository.findDistinctTyNM());
    }

    @Cacheable("recipeIngredientCategories")
    public List<String> getRecipeIrdntCODE(){
        return normalizeCategoryValues(infoRepository.findDistinctByIrdntCODEIsNotNull());
    }


    //////////////////// 전체 레시피 리스트 ///////////////////////////////

    public Page<Recipe_INFO_ResponseDto> getAllRecipeInfo(Pageable pageable) {
        return infoRepository.findAll(pageable).map(this::toInfoResponse); // 레시피 리스트 표시
    }

    public Page<Recipe_INFO_ResponseDto> getFilteredRecipeList(List<String> type,
                                                               List<String> nation,
                                                               List<String> ingredient,
                                                               String keyword,
                                                               String search,
                                                               Pageable pageable) {
        return infoRepository.findRecipesByConditions(type, nation, ingredient, keyword, search, pageable)
                .map(this::toInfoResponse); //검색 필터링

    }

    public Recipe_INFO_ResponseDto toInfoResponse(Recipe_INFO recipeInfo) {
        return Recipe_INFO_ResponseDto.builder()
                .rowNUM(recipeInfo.getRowNUM())
                .recipeID(recipeInfo.getRecipeID())
                .recipeNMKO(recipeInfo.getRecipeNMKO())
                .sumry(recipeInfo.getSumry())
                .nationCODE(recipeInfo.getNationCODE())
                .nationNM(recipeInfo.getNationNM())
                .tyCODE(recipeInfo.getTyCODE())
                .tyNM(recipeInfo.getTyNM())
                .cookingTIME(recipeInfo.getCookingTIME())
                .calorie(recipeInfo.getCalorie())
                .qnt(recipeInfo.getQnt())
                .levelNM(recipeInfo.getLevelNM())
                .irdntCODE(recipeInfo.getIrdntCODE())
                .pcNM(recipeInfo.getPcNM())
                .imgURL(recipeInfo.getImgURL())
                .build();
    }

    private Recipe_CRSE_ResponseDto toCrseResponse(Recipe_CRSE recipeCrse) {
        return Recipe_CRSE_ResponseDto.builder()
                .rowNUM(recipeCrse.getRowNUM())
                .recipeID(recipeCrse.getRecipeID())
                .cookingNO(recipeCrse.getCookingNO())
                .cookingDC(recipeCrse.getCookingDC())
                .stepTIP(recipeCrse.getStepTIP())
                .imgURL(recipeCrse.getImgURL())
                .build();
    }

    private Recipe_IRDNT_ResponseDto toIrdntResponse(Recipe_IRDNT recipeIrdnt) {
        return Recipe_IRDNT_ResponseDto.builder()
                .rowNUM(recipeIrdnt.getRowNUM())
                .recipeID(recipeIrdnt.getRecipeID())
                .irdntSN(recipeIrdnt.getIrdntSN())
                .irdntNM(recipeIrdnt.getIrdntNM())
                .irdntCPCTY(recipeIrdnt.getIrdntCPCTY())
                .irdntTYCODE(recipeIrdnt.getIrdntTYCODE())
                .irdntTYNM(recipeIrdnt.getIrdntTYNM())
                .build();
    }

    private List<String> normalizeCategoryValues(List<String> categories) {
        return categories.stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted()
                .toList();
    }
}
