package kwh.PublicCookedFood.food.facade;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.service.BoardService;
import kwh.PublicCookedFood.common.dto.request.RecipeSearchQuery;
import kwh.PublicCookedFood.food.dto.response.RecipeCategoryGroupResponse;
import kwh.PublicCookedFood.food.dto.response.RecipeRankingResponse;
import kwh.PublicCookedFood.food.dto.response.recipe_info.Recipe_INFO_ResponseDto;
import kwh.PublicCookedFood.food.service.RecipeReviewService;
import kwh.PublicCookedFood.food.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecipeMainFacade {

    private static final String QUERY_INVALID_MESSAGE = "검색 조건이 유효하지 않아 기본 목록을 표시합니다.";

    private final RecipeService recipeService;
    private final BoardService boardService;
    private final RecipeReviewService recipeReviewService;

    public HomeViewData loadHomeData() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);
        List<Board> popularBoards = boardService.getPopularBoardsSince(weekAgo, 6);
        List<RecipeRankingResponse> recipeRankings = recipeReviewService.getTopReviewRankings(monthAgo, 6);
        return new HomeViewData(popularBoards, recipeRankings);
    }

    public RecipeListViewData loadRecipeList(Pageable pageable,
                                             RecipeSearchQuery query,
                                             boolean hasBindingErrors) {
        List<String> type = query.normalizedType();
        List<String> nation = query.normalizedNation();
        List<String> ingredient = query.normalizedIngredient();
        String keyword = query.normalizedKeyword();
        String search = query.normalizedSearch();
        String queryErrorMsg = null;

        if (hasBindingErrors) {
            type = List.of();
            nation = List.of();
            ingredient = List.of();
            keyword = null;
            search = null;
            queryErrorMsg = QUERY_INVALID_MESSAGE;
        }

        Page<Recipe_INFO_ResponseDto> recipePage = getRecipeInfo(
                pageable,
                type,
                nation,
                ingredient,
                keyword,
                search
        );

        return new RecipeListViewData(
                recipePage,
                type,
                nation,
                ingredient,
                keyword,
                search,
                createCategories(),
                queryErrorMsg
        );
    }

    private Page<Recipe_INFO_ResponseDto> getRecipeInfo(Pageable pageable,
                                                        List<String> type,
                                                        List<String> nation,
                                                        List<String> ingredient,
                                                        String keyword,
                                                        String search) {
        if ((type == null || type.isEmpty())
                && (nation == null || nation.isEmpty())
                && (ingredient == null || ingredient.isEmpty())
                && (keyword == null || keyword.isEmpty())
                && (search == null || search.isEmpty())) {
            return recipeService.getAllRecipeInfo(pageable);
        }
        return recipeService.getFilteredRecipeList(type, nation, ingredient, keyword, search, pageable);
    }

    private List<RecipeCategoryGroupResponse> createCategories() {
        List<String> allTyNmList = recipeService.getRecipeTypeNames();
        List<String> allNationNmList = recipeService.getRecipeNationNames();
        List<String> allIrdntCodeList = recipeService.getRecipeIrdntCODE();

        return List.of(
                new RecipeCategoryGroupResponse("음식별", "nation", allNationNmList),
                new RecipeCategoryGroupResponse("재료별", "ingredient", allIrdntCodeList),
                new RecipeCategoryGroupResponse("분류별", "type", allTyNmList)
        );
    }

    public record HomeViewData(List<Board> popularBoards,
                               List<RecipeRankingResponse> recipeRankings) {
    }

    public record RecipeListViewData(Page<Recipe_INFO_ResponseDto> recipePage,
                                     List<String> selectedTypes,
                                     List<String> selectedNations,
                                     List<String> selectedIngredients,
                                     String selectedKeyword,
                                     String selectedSearch,
                                     List<RecipeCategoryGroupResponse> categories,
                                     String queryErrorMsg) {
    }
}
