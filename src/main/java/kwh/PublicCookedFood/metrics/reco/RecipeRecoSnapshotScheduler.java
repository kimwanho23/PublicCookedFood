package kwh.PublicCookedFood.metrics.reco;

import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.food.repository.Recipe_INFO_Repository;
import kwh.PublicCookedFood.metrics.view.RecipeStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecipeRecoSnapshotScheduler {

    private static final String DEFAULT_LUNCH_KEYWORDS = "밥,국,찌개,면,볶음,덮밥";
    private static final String DEFAULT_DINNER_KEYWORDS = "구이,찜,탕,전골,조림,볶음";

    private final RecipeStatsRepository recipeStatsRepository;
    private final RecipeRecoSnapshotService recipeRecoSnapshotService;
    private final Recipe_INFO_Repository recipeInfoRepository;

    @Value("${app.popular.recipe.snapshot.enabled:false}")
    private boolean enabled;

    @Value("${app.popular.recipe.snapshot.top-n:100}")
    private int topN;

    @Value("${app.popular.recipe.snapshot.ttl-minutes:30}")
    private int ttlMinutes;

    @Value("${app.popular.recipe.snapshot.slot-bonus:0.05}")
    private BigDecimal slotBonus;

    @Value("${app.popular.recipe.snapshot.lunch-keywords:" + DEFAULT_LUNCH_KEYWORDS + "}")
    private String lunchKeywordsRaw;

    @Value("${app.popular.recipe.snapshot.dinner-keywords:" + DEFAULT_DINNER_KEYWORDS + "}")
    private String dinnerKeywordsRaw;

    @Scheduled(fixedDelayString = "${app.popular.recipe.snapshot.interval-ms:300000}")
    public void generateRecipeRecommendations() {
        if (!enabled) {
            return;
        }

        int normalizedTopN = Math.max(1, topN);
        int normalizedTtlMinutes = Math.max(1, ttlMinutes);
        LocalDateTime generatedAt = LocalDateTime.now();
        LocalDateTime expiresAt = generatedAt.plusMinutes(normalizedTtlMinutes);
        LocalDate slotDate = generatedAt.toLocalDate();

        List<RecipeStatsRepository.RecipeScoreProjection> rankedRecipes =
                recipeStatsRepository.findTopRecipeScoresForSnapshot(PageRequest.of(0, normalizedTopN));
        if (rankedRecipes == null) {
            rankedRecipes = List.of();
        }
        Map<Long, BigDecimal> rankedScoreByRecipeId = new LinkedHashMap<>();
        for (RecipeStatsRepository.RecipeScoreProjection rankedRecipe : rankedRecipes) {
            if (rankedRecipe == null || rankedRecipe.getRecipeId() == null) {
                continue;
            }
            rankedScoreByRecipeId.putIfAbsent(
                    rankedRecipe.getRecipeId(),
                    rankedRecipe.getScore() == null ? BigDecimal.ZERO : rankedRecipe.getScore()
            );
        }
        List<RecipeRecoSnapshotService.RecommendationCandidate> baseCandidates = rankedScoreByRecipeId.entrySet().stream()
                .map(entry -> new RecipeRecoSnapshotService.RecommendationCandidate(entry.getKey(), entry.getValue()))
                .toList();
        Map<Long, Recipe_INFO> recipeInfoByRecipeId = loadRecipeInfoMap(baseCandidates);
        List<RecipeRecoSnapshotService.RecommendationCandidate> lunchCandidates =
                adjustCandidatesBySlot(baseCandidates, recipeInfoByRecipeId, RecipeRecoSnapshotService.SLOT_LUNCH, normalizedTopN);
        List<RecipeRecoSnapshotService.RecommendationCandidate> dinnerCandidates =
                adjustCandidatesBySlot(baseCandidates, recipeInfoByRecipeId, RecipeRecoSnapshotService.SLOT_DINNER, normalizedTopN);

        recipeRecoSnapshotService.replaceSlot(
                slotDate,
                RecipeRecoSnapshotService.SLOT_LUNCH,
                lunchCandidates,
                generatedAt,
                expiresAt
        );
        recipeRecoSnapshotService.replaceSlot(
                slotDate,
                RecipeRecoSnapshotService.SLOT_DINNER,
                dinnerCandidates,
                generatedAt,
                expiresAt
        );

        int deletedExpired = recipeRecoSnapshotService.deleteExpiredSnapshots();
        log.debug("Generated recipe reco snapshots. lunchCandidates={}, dinnerCandidates={}, expiredDeleted={}",
                lunchCandidates.size(),
                dinnerCandidates.size(),
                deletedExpired);
    }

    private Map<Long, Recipe_INFO> loadRecipeInfoMap(List<RecipeRecoSnapshotService.RecommendationCandidate> baseCandidates) {
        if (baseCandidates == null || baseCandidates.isEmpty()) {
            return Map.of();
        }

        List<Long> recipeIds = baseCandidates.stream()
                .map(RecipeRecoSnapshotService.RecommendationCandidate::recipeId)
                .filter(Objects::nonNull)
                .toList();
        if (recipeIds.isEmpty()) {
            return Map.of();
        }

        List<Recipe_INFO> recipes = recipeInfoRepository.findAllByRecipeIDIn(recipeIds);
        if (recipes == null || recipes.isEmpty()) {
            return Map.of();
        }

        Map<Long, Recipe_INFO> recipeById = new LinkedHashMap<>();
        for (Recipe_INFO recipe : recipes) {
            if (recipe == null || recipe.getRecipeID() == null) {
                continue;
            }
            recipeById.putIfAbsent(recipe.getRecipeID(), recipe);
        }
        return recipeById;
    }

    private List<RecipeRecoSnapshotService.RecommendationCandidate> adjustCandidatesBySlot(
            List<RecipeRecoSnapshotService.RecommendationCandidate> baseCandidates,
            Map<Long, Recipe_INFO> recipeInfoByRecipeId,
            String slotType,
            int topN
    ) {
        if (baseCandidates == null || baseCandidates.isEmpty()) {
            return List.of();
        }

        List<String> keywords = RecipeRecoSnapshotService.SLOT_DINNER.equals(slotType)
                ? parseKeywords(dinnerKeywordsRaw, DEFAULT_DINNER_KEYWORDS)
                : parseKeywords(lunchKeywordsRaw, DEFAULT_LUNCH_KEYWORDS);
        BigDecimal normalizedSlotBonus = slotBonus == null ? BigDecimal.ZERO : slotBonus.max(BigDecimal.ZERO);

        List<SlotAdjustedCandidate> adjusted = new java.util.ArrayList<>();
        for (int i = 0; i < baseCandidates.size(); i++) {
            RecipeRecoSnapshotService.RecommendationCandidate candidate = baseCandidates.get(i);
            if (candidate == null || candidate.recipeId() == null) {
                continue;
            }
            BigDecimal baseScore = candidate.score() == null ? BigDecimal.ZERO : candidate.score();
            Recipe_INFO recipe = recipeInfoByRecipeId.get(candidate.recipeId());
            int matchCount = countKeywordMatches(recipe, keywords);
            BigDecimal adjustedScore = baseScore.add(normalizedSlotBonus.multiply(BigDecimal.valueOf(matchCount)));
            adjusted.add(new SlotAdjustedCandidate(candidate.recipeId(), adjustedScore, baseScore, i));
        }

        return adjusted.stream()
                .sorted(Comparator
                        .comparing(SlotAdjustedCandidate::adjustedScore, Comparator.reverseOrder())
                        .thenComparing(SlotAdjustedCandidate::baseScore, Comparator.reverseOrder())
                        .thenComparing(SlotAdjustedCandidate::baseOrder))
                .limit(Math.max(1, topN))
                .map(item -> new RecipeRecoSnapshotService.RecommendationCandidate(item.recipeId(), item.adjustedScore()))
                .toList();
    }

    private int countKeywordMatches(Recipe_INFO recipe, List<String> keywords) {
        if (recipe == null || keywords == null || keywords.isEmpty()) {
            return 0;
        }

        String recipeName = normalizeText(recipe.getRecipeNMKO());
        String typeName = normalizeText(recipe.getTyNM());
        String nationName = normalizeText(recipe.getNationNM());
        String ingredientCode = normalizeText(recipe.getIrdntCODE());

        int matches = 0;
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            String normalizedKeyword = normalizeText(keyword);
            if (recipeName.contains(normalizedKeyword)
                    || typeName.contains(normalizedKeyword)
                    || nationName.contains(normalizedKeyword)
                    || ingredientCode.contains(normalizedKeyword)) {
                matches++;
            }
        }
        return matches;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private List<String> parseKeywords(String rawKeywords, String defaults) {
        String source = (rawKeywords == null || rawKeywords.isBlank()) ? defaults : rawKeywords;
        return Arrays.stream(source.split(","))
                .map(String::trim)
                .filter(keyword -> !keyword.isEmpty())
                .toList();
    }

    private record SlotAdjustedCandidate(Long recipeId,
                                         BigDecimal adjustedScore,
                                         BigDecimal baseScore,
                                         int baseOrder) {
    }
}
