package kwh.PublicCookedFood.food.service;

import kwh.PublicCookedFood.food.dto.recipe_crse.Recipe_CRSE_ResponseDto;
import kwh.PublicCookedFood.food.dto.recipe_info.Recipe_INFO_ResponseDto;
import kwh.PublicCookedFood.food.dto.recipe_irdnt.Recipe_IRDNT_ResponseDto;
import kwh.PublicCookedFood.food.entity.Recipe_CRSE;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.food.entity.Recipe_IRDNT;
import kwh.PublicCookedFood.food.repository.Recipe_CRSE_Repository;
import kwh.PublicCookedFood.food.repository.Recipe_INFO_Repository;
import kwh.PublicCookedFood.food.repository.Recipe_IRDNT_Repository;
import lombok.RequiredArgsConstructor;
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
    private final Recipe_INFO_Repository recipe_INFO_Repository;

    public List<Recipe_CRSE_ResponseDto> getRecipe_CRSE(Long id){
        return crseRepository.findAllByRecipeIDOrderByCookingNOAsc(id)
                .stream()
                .map(Recipe_CRSE::toResponseDto)
                .toList(); //레시피 과정정보
    }

    public Recipe_INFO_ResponseDto getRecipe_INFO(Long id){
        return infoRepository.findByRecipeID(id)
                .toResponseDto(); //모든 레시피 기본정보
    }

    public List<Recipe_IRDNT_ResponseDto> getRecipe_IRDNT(Long recipeId){
        return irdntRepository.findAllByRecipeIDOrderByIrdntTYCODEAsc(recipeId)
                .stream()
                .map(Recipe_IRDNT::toResponseDto)
                .toList(); //레시피 재료정보
    }



    /////////////////   카테고리명   ////////////////////////
    public List<String> getRecipeNation_NM(){
        return infoRepository.findDistinctNationNM();
    }

    public List<String> getRecipeTy_NM(){
        return infoRepository.findDistinctTyNM();
    }

    public List<String> getRecipeIrdntCODE(){
        return infoRepository.findDistinctByIrdntCODEIsNotNull();
    }


    //////////////////// 전체 레시피 리스트 ///////////////////////////////

    public Page<Recipe_INFO_ResponseDto> getAllRecipe_INFO(Pageable pageable) {
        return infoRepository.findAll(pageable).map(Recipe_INFO::toResponseDto); // 레시피 리스트 표시
    }

    public Page<Recipe_INFO_ResponseDto> getFilteredRecipeList(String keyword, String search, Pageable pageable) {
        return infoRepository.findRecipesByKeywordAndSearch(keyword, search, pageable).map(Recipe_INFO::toResponseDto); //검색 필터링

    }
}
