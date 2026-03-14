package kwh.PublicCookedFood.food.facade;

import kwh.PublicCookedFood.board.application.query.BoardCardViewAssembler;
import kwh.PublicCookedFood.board.application.query.view.BoardCardView;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.service.query.BoardPopularityQueryService;
import kwh.PublicCookedFood.board.service.support.BoardStatsSummary;
import kwh.PublicCookedFood.board.service.support.BoardStatsSummaryResolver;
import kwh.PublicCookedFood.food.dto.request.RecipeSearchQuery;
import kwh.PublicCookedFood.food.dto.response.RecipeCategoryGroupResponse;
import kwh.PublicCookedFood.food.dto.response.RecipeRankingResponse;
import kwh.PublicCookedFood.food.dto.response.recipe_info.Recipe_INFO_ResponseDto;
import kwh.PublicCookedFood.metrics.reco.RecipeRecoSnapshotService;
import kwh.PublicCookedFood.food.service.RecipeReviewService;
import kwh.PublicCookedFood.food.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecipeMainFacade {

    private static final String QUERY_INVALID_MESSAGE = "검색 조건이 유효하지 않아 기본 목록을 표시합니다.";

    private final RecipeService recipeService;
    private final BoardPopularityQueryService boardPopularityQueryService;
    private final RecipeReviewService recipeReviewService;
    private final RecipeRecoSnapshotService recipeRecoSnapshotService;
    private final BoardStatsSummaryResolver boardStatsSummaryResolver;
    private final BoardCardViewAssembler boardCardViewAssembler;

    public HomeViewData loadHomeData() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);
        List<Board> popularBoards = boardPopularityQueryService.getPopularBoardsSince(weekAgo, 6);
        List<RecipeRankingResponse> recipeRankings = recipeReviewService.getTopReviewRankings(monthAgo, 6);
        List<RecipeRecoSnapshotService.RecipeRecommendationItem> lunchRecommendations =
                recipeRecoSnapshotService.loadRecommendations(RecipeRecoSnapshotService.SLOT_LUNCH, 6);
        List<RecipeRecoSnapshotService.RecipeRecommendationItem> dinnerRecommendations =
                recipeRecoSnapshotService.loadRecommendations(RecipeRecoSnapshotService.SLOT_DINNER, 6);
        Map<Long, BoardStatsSummary> boardStatsMap = boardStatsSummaryResolver.resolve(popularBoards);
        return new HomeViewData(
                boardCardViewAssembler.toList(popularBoards, boardStatsMap),
                recipeRankings,
                lunchRecommendations,
                dinnerRecommendations
        );
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

        LegacyKeywordResolution legacyKeywordResolution = resolveLegacyKeyword(type, nation, ingredient, keyword);
        type = legacyKeywordResolution.type();
        nation = legacyKeywordResolution.nation();
        ingredient = legacyKeywordResolution.ingredient();
        keyword = legacyKeywordResolution.keyword();

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

    private LegacyKeywordResolution resolveLegacyKeyword(List<String> type,
                                                         List<String> nation,
                                                         List<String> ingredient,
                                                         String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return new LegacyKeywordResolution(type, nation, ingredient, keyword);
        }
        if ((type != null && !type.isEmpty())
                || (nation != null && !nation.isEmpty())
                || (ingredient != null && !ingredient.isEmpty())) {
            return new LegacyKeywordResolution(type, nation, ingredient, keyword);
        }

        List<String> resolvedType = new ArrayList<>(type == null ? List.of() : type);
        List<String> resolvedNation = new ArrayList<>(nation == null ? List.of() : nation);
        List<String> resolvedIngredient = new ArrayList<>(ingredient == null ? List.of() : ingredient);

        String typeMatch = findCanonicalMatch(recipeService.getRecipeTypeNames(), keyword);
        String nationMatch = findCanonicalMatch(recipeService.getRecipeNationNames(), keyword);
        String ingredientMatch = findCanonicalMatch(recipeService.getRecipeIrdntCODE(), keyword);

        int matchCount = 0;
        if (typeMatch != null) {
            matchCount++;
        }
        if (nationMatch != null) {
            matchCount++;
        }
        if (ingredientMatch != null) {
            matchCount++;
        }

        if (matchCount != 1) {
            return new LegacyKeywordResolution(type, nation, ingredient, keyword);
        }

        if (typeMatch != null) {
            resolvedType.add(typeMatch);
        } else if (nationMatch != null) {
            resolvedNation.add(nationMatch);
        } else {
            resolvedIngredient.add(ingredientMatch);
        }
        return new LegacyKeywordResolution(resolvedType, resolvedNation, resolvedIngredient, null);
    }

    private String findCanonicalMatch(List<String> candidates, String keyword) {
        if (candidates == null || candidates.isEmpty() || keyword == null) {
            return null;
        }
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        if (normalizedKeyword.isEmpty()) {
            return null;
        }
        return candidates.stream()
                .filter(candidate -> candidate != null
                        && candidate.trim().toLowerCase(Locale.ROOT).equals(normalizedKeyword))
                .findFirst()
                .orElse(null);
    }

    private record LegacyKeywordResolution(List<String> type,
                                           List<String> nation,
                                           List<String> ingredient,
                                           String keyword) {
    }

    public record HomeViewData(List<BoardCardView> popularBoards,
                               List<RecipeRankingResponse> recipeRankings,
                               List<RecipeRecoSnapshotService.RecipeRecommendationItem> lunchRecommendations,
                               List<RecipeRecoSnapshotService.RecipeRecommendationItem> dinnerRecommendations) {
        public HomeViewData {
            popularBoards = popularBoards == null ? List.of() : List.copyOf(popularBoards);
            recipeRankings = recipeRankings == null ? List.of() : List.copyOf(recipeRankings);
            lunchRecommendations = lunchRecommendations == null ? List.of() : List.copyOf(lunchRecommendations);
            dinnerRecommendations = dinnerRecommendations == null ? List.of() : List.copyOf(dinnerRecommendations);
        }
    }

    public record RecipeListViewData(Page<Recipe_INFO_ResponseDto> recipePage,
                                     List<String> selectedTypes,
                                     List<String> selectedNations,
                                     List<String> selectedIngredients,
                                     String selectedKeyword,
                                     String selectedSearch,
                                     List<RecipeCategoryGroupResponse> categories,
                                     String queryErrorMsg) {
        public RecipeListViewData {
            selectedTypes = selectedTypes == null ? List.of() : List.copyOf(selectedTypes);
            selectedNations = selectedNations == null ? List.of() : List.copyOf(selectedNations);
            selectedIngredients = selectedIngredients == null ? List.of() : List.copyOf(selectedIngredients);
            categories = categories == null ? List.of() : List.copyOf(categories);
        }
    }
}
