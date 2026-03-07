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
import java.util.ArrayList;
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
    private static final int SLOT_CANDIDATE_POOL_MULTIPLIER = 5;
    private static final int MAX_STATS_FETCH_PAGES = 20;

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
        int candidatePoolSize = resolveCandidatePoolSize(normalizedTopN);
        int normalizedTtlMinutes = Math.max(1, ttlMinutes);
        LocalDateTime generatedAt = LocalDateTime.now();
        LocalDateTime expiresAt = generatedAt.plusMinutes(normalizedTtlMinutes);
        LocalDate slotDate = generatedAt.toLocalDate();

        ValidRecommendationBatch validRecommendationBatch = loadValidRecommendationBatch(candidatePoolSize);
        List<RecipeRecoSnapshotService.RecommendationCandidate> existingCandidates = validRecommendationBatch.candidates();
        Map<Long, Recipe_INFO> recipeInfoByRecipeId = validRecommendationBatch.recipeInfoByRecipeId();
        List<RecipeRecoSnapshotService.RecommendationCandidate> lunchCandidates =
                adjustCandidatesBySlot(existingCandidates, recipeInfoByRecipeId, RecipeRecoSnapshotService.SLOT_LUNCH, normalizedTopN);
        List<RecipeRecoSnapshotService.RecommendationCandidate> dinnerCandidates =
                adjustCandidatesBySlot(existingCandidates, recipeInfoByRecipeId, RecipeRecoSnapshotService.SLOT_DINNER, normalizedTopN);

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

    private ValidRecommendationBatch loadValidRecommendationBatch(int candidatePoolSize) {
        if (candidatePoolSize <= 0) {
            return new ValidRecommendationBatch(List.of(), Map.of());
        }

        List<RecipeRecoSnapshotService.RecommendationCandidate> candidates = new ArrayList<>();
        Map<Long, Recipe_INFO> recipeInfoByRecipeId = new LinkedHashMap<>();
        Map<Long, BigDecimal> seenScoreByRecipeId = new LinkedHashMap<>();

        for (int page = 0; page < MAX_STATS_FETCH_PAGES && candidates.size() < candidatePoolSize; page++) {
            List<RecipeStatsRepository.RecipeScoreProjection> rankedRecipes =
                    recipeStatsRepository.findTopRecipeScoresForSnapshot(PageRequest.of(page, candidatePoolSize));
            if (rankedRecipes == null || rankedRecipes.isEmpty()) {
                break;
            }

            List<Long> batchIds = new ArrayList<>();
            Map<Long, BigDecimal> batchScores = new LinkedHashMap<>();
            for (RecipeStatsRepository.RecipeScoreProjection rankedRecipe : rankedRecipes) {
                if (rankedRecipe == null || rankedRecipe.getRecipeId() == null) {
                    continue;
                }
                Long recipeId = rankedRecipe.getRecipeId();
                if (seenScoreByRecipeId.containsKey(recipeId) || batchScores.containsKey(recipeId)) {
                    continue;
                }
                batchIds.add(recipeId);
                batchScores.put(recipeId, rankedRecipe.getScore() == null ? BigDecimal.ZERO : rankedRecipe.getScore());
            }

            if (!batchIds.isEmpty()) {
                Map<Long, Recipe_INFO> batchRecipeInfo = recipeInfoRepository.findAllByRecipeIDIn(batchIds).stream()
                        .filter(recipe -> recipe != null && recipe.getRecipeID() != null)
                        .collect(java.util.stream.Collectors.toMap(
                                Recipe_INFO::getRecipeID,
                                recipe -> recipe,
                                (left, right) -> left,
                                LinkedHashMap::new
                        ));

                for (Long recipeId : batchIds) {
                    Recipe_INFO recipe = batchRecipeInfo.get(recipeId);
                    if (recipe == null) {
                        continue;
                    }
                    BigDecimal score = batchScores.getOrDefault(recipeId, BigDecimal.ZERO);
                    seenScoreByRecipeId.put(recipeId, score);
                    recipeInfoByRecipeId.put(recipeId, recipe);
                    candidates.add(new RecipeRecoSnapshotService.RecommendationCandidate(recipeId, score));
                    if (candidates.size() >= candidatePoolSize) {
                        break;
                    }
                }
            }

            if (rankedRecipes.size() < candidatePoolSize) {
                break;
            }
        }

        return new ValidRecommendationBatch(candidates, recipeInfoByRecipeId);
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
            if (recipe == null) {
                continue;
            }
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

    private int resolveCandidatePoolSize(int normalizedTopN) {
        if (normalizedTopN <= 0) {
            return 1;
        }
        if (normalizedTopN > Integer.MAX_VALUE / SLOT_CANDIDATE_POOL_MULTIPLIER) {
            return Integer.MAX_VALUE;
        }
        return Math.max(normalizedTopN, normalizedTopN * SLOT_CANDIDATE_POOL_MULTIPLIER);
    }

    private record SlotAdjustedCandidate(Long recipeId,
                                         BigDecimal adjustedScore,
                                         BigDecimal baseScore,
                                         int baseOrder) {
    }

    private record ValidRecommendationBatch(List<RecipeRecoSnapshotService.RecommendationCandidate> candidates,
                                            Map<Long, Recipe_INFO> recipeInfoByRecipeId) {
    }
}
