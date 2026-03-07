package kwh.PublicCookedFood.metrics.reco;

import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.food.repository.Recipe_INFO_Repository;
import kwh.PublicCookedFood.metrics.view.RecipeStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeRecoSnapshotService {

    public static final String SLOT_LUNCH = "LUNCH";
    public static final String SLOT_DINNER = "DINNER";
    private static final String DEFAULT_LUNCH_KEYWORDS = "밥,국,찌개,면,볶음,덮밥";
    private static final String DEFAULT_DINNER_KEYWORDS = "구이,찜,탕,전골,조림,볶음";
    private static final int FALLBACK_CANDIDATE_POOL_MULTIPLIER = 5;
    private static final int MAX_STATS_FETCH_PAGES = 20;

    private final RecipeRecoSnapshotRepository snapshotRepository;
    private final Recipe_INFO_Repository recipeInfoRepository;
    private final RecipeStatsRepository recipeStatsRepository;

    @Value("${app.popular.recipe.snapshot.fallback-enabled:true}")
    private boolean fallbackEnabled;

    @Value("${app.popular.recipe.snapshot.slot-bonus:0.05}")
    private BigDecimal slotBonus;

    @Value("${app.popular.recipe.snapshot.lunch-keywords:" + DEFAULT_LUNCH_KEYWORDS + "}")
    private String lunchKeywordsRaw;

    @Value("${app.popular.recipe.snapshot.dinner-keywords:" + DEFAULT_DINNER_KEYWORDS + "}")
    private String dinnerKeywordsRaw;

    @Transactional(readOnly = true)
    public List<RecipeRecommendationItem> loadRecommendations(String slotType, int limit) {
        String normalizedSlot = normalizeSlotType(slotType);
        if (limit <= 0) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        List<RecipeRecoSnapshot> snapshotRows;
        int fetchSize = resolveCandidatePoolSize(limit);
        try {
            snapshotRows = snapshotRepository.findActiveBySlot(
                    today,
                    normalizedSlot,
                    now,
                    Pageable.ofSize(fetchSize)
            );
        } catch (RuntimeException e) {
            log.warn("Failed to load recipe recommendation snapshot. slotType={}, limit={}",
                    normalizedSlot, limit, e);
            return loadFallbackOrEmpty(normalizedSlot, limit);
        }
        if (snapshotRows.isEmpty()) {
            return loadFallbackOrEmpty(normalizedSlot, limit);
        }

        LinkedHashSet<Long> orderedIdSet = snapshotRows.stream()
                .map(RecipeRecoSnapshot::getRecipeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (orderedIdSet.isEmpty()) {
            return loadFallbackOrEmpty(normalizedSlot, limit);
        }
        List<Long> orderedIds = new ArrayList<>(orderedIdSet);

        List<Recipe_INFO> recipes = recipeInfoRepository.findAllByRecipeIDIn(orderedIds);
        Map<Long, Recipe_INFO> recipeById = new LinkedHashMap<>();
        for (Recipe_INFO recipe : recipes) {
            if (recipe == null || recipe.getRecipeID() == null) {
                continue;
            }
            recipeById.put(recipe.getRecipeID(), recipe);
        }

        Map<Long, BigDecimal> scoreByRecipeId = new LinkedHashMap<>();
        for (RecipeRecoSnapshot row : snapshotRows) {
            if (row == null || row.getRecipeId() == null) {
                continue;
            }
            scoreByRecipeId.putIfAbsent(row.getRecipeId(), row.getScore() == null ? BigDecimal.ZERO : row.getScore());
        }

        List<RecipeRecommendationItem> orderedItems = new ArrayList<>();
        for (Long recipeId : orderedIds) {
            Recipe_INFO recipe = recipeById.get(recipeId);
            if (recipe == null) {
                continue;
            }
            orderedItems.add(new RecipeRecommendationItem(
                    recipe.getRecipeID(),
                    recipe.getRecipeNMKO(),
                    recipe.getImgURL(),
                    scoreByRecipeId.getOrDefault(recipeId, BigDecimal.ZERO)
            ));
        }
        if (orderedItems.isEmpty()) {
            return loadFallbackOrEmpty(normalizedSlot, limit);
        }
        if (orderedItems.size() >= limit) {
            return orderedItems.stream()
                    .limit(limit)
                    .toList();
        }
        return appendFallbackRecommendations(normalizedSlot, limit, orderedItems);
    }

    private List<RecipeRecommendationItem> loadFallbackOrEmpty(String slotType, int limit) {
        if (!fallbackEnabled) {
            return List.of();
        }
        return loadFallbackRecommendations(slotType, limit);
    }

    private List<RecipeRecommendationItem> loadFallbackRecommendations(String slotType, int limit) {
        return loadFallbackRecommendations(slotType, limit, Set.of());
    }

    private List<RecipeRecommendationItem> loadFallbackRecommendations(String slotType,
                                                                       int limit,
                                                                       Set<Long> excludedRecipeIds) {
        if (limit <= 0) {
            return List.of();
        }

        int excludedCount = excludedRecipeIds == null ? 0 : excludedRecipeIds.size();
        int candidatePoolSize = resolveCandidatePoolSize(limit + excludedCount);
        ValidRecipeScoreBatch validCandidates = loadValidFallbackCandidates(candidatePoolSize, excludedRecipeIds);
        if (validCandidates.orderedIds().isEmpty()) {
            return List.of();
        }

        List<String> keywords = SLOT_DINNER.equals(slotType)
                ? parseKeywords(dinnerKeywordsRaw, DEFAULT_DINNER_KEYWORDS)
                : parseKeywords(lunchKeywordsRaw, DEFAULT_LUNCH_KEYWORDS);
        BigDecimal normalizedSlotBonus = slotBonus == null ? BigDecimal.ZERO : slotBonus.max(BigDecimal.ZERO);

        List<SlotAdjustedRecipe> adjustedRecipes = new ArrayList<>();
        for (int i = 0; i < validCandidates.orderedIds().size(); i++) {
            Long recipeId = validCandidates.orderedIds().get(i);
            Recipe_INFO recipe = validCandidates.recipeById().get(recipeId);
            if (recipe == null) {
                continue;
            }
            BigDecimal baseScore = validCandidates.scoreByRecipeId().getOrDefault(recipeId, BigDecimal.ZERO);
            int matchCount = countKeywordMatches(recipe, keywords);
            BigDecimal adjustedScore = baseScore.add(normalizedSlotBonus.multiply(BigDecimal.valueOf(matchCount)));
            adjustedRecipes.add(new SlotAdjustedRecipe(recipe, adjustedScore, baseScore, i));
        }

        return adjustedRecipes.stream()
                .sorted(Comparator
                        .comparing(SlotAdjustedRecipe::adjustedScore, Comparator.reverseOrder())
                        .thenComparing(SlotAdjustedRecipe::baseScore, Comparator.reverseOrder())
                        .thenComparing(SlotAdjustedRecipe::baseOrder))
                .limit(limit)
                .map(item -> new RecipeRecommendationItem(
                        item.recipe().getRecipeID(),
                        item.recipe().getRecipeNMKO(),
                        item.recipe().getImgURL(),
                        item.adjustedScore()))
                .toList();
    }

    private ValidRecipeScoreBatch loadValidFallbackCandidates(int candidatePoolSize, Set<Long> excludedRecipeIds) {
        if (candidatePoolSize <= 0) {
            return new ValidRecipeScoreBatch(List.of(), Map.of(), Map.of());
        }

        Set<Long> excluded = excludedRecipeIds == null ? Set.of() : Set.copyOf(excludedRecipeIds);
        List<Long> orderedIds = new ArrayList<>();
        Map<Long, BigDecimal> scoreByRecipeId = new LinkedHashMap<>();
        Map<Long, Recipe_INFO> recipeById = new LinkedHashMap<>();

        for (int page = 0; page < MAX_STATS_FETCH_PAGES && orderedIds.size() < candidatePoolSize; page++) {
            List<RecipeStatsRepository.RecipeScoreProjection> ranked =
                    recipeStatsRepository.findTopRecipeScoresForSnapshot(PageRequest.of(page, candidatePoolSize));
            if (ranked == null || ranked.isEmpty()) {
                break;
            }

            List<Long> batchIds = new ArrayList<>();
            Map<Long, BigDecimal> batchScores = new LinkedHashMap<>();
            for (RecipeStatsRepository.RecipeScoreProjection row : ranked) {
                if (row == null || row.getRecipeId() == null) {
                    continue;
                }
                Long recipeId = row.getRecipeId();
                if (excluded.contains(recipeId) || scoreByRecipeId.containsKey(recipeId) || batchScores.containsKey(recipeId)) {
                    continue;
                }
                batchIds.add(recipeId);
                batchScores.put(recipeId, row.getScore() == null ? BigDecimal.ZERO : row.getScore());
            }

            if (!batchIds.isEmpty()) {
                Map<Long, Recipe_INFO> batchRecipeById = recipeInfoRepository.findAllByRecipeIDIn(batchIds).stream()
                        .filter(recipe -> recipe != null && recipe.getRecipeID() != null)
                        .collect(Collectors.toMap(
                                Recipe_INFO::getRecipeID,
                                recipe -> recipe,
                                (left, right) -> left,
                                LinkedHashMap::new
                        ));

                for (Long recipeId : batchIds) {
                    Recipe_INFO recipe = batchRecipeById.get(recipeId);
                    if (recipe == null) {
                        continue;
                    }
                    orderedIds.add(recipeId);
                    scoreByRecipeId.put(recipeId, batchScores.getOrDefault(recipeId, BigDecimal.ZERO));
                    recipeById.put(recipeId, recipe);
                    if (orderedIds.size() >= candidatePoolSize) {
                        break;
                    }
                }
            }

            if (ranked.size() < candidatePoolSize) {
                break;
            }
        }

        return new ValidRecipeScoreBatch(orderedIds, scoreByRecipeId, recipeById);
    }

    private List<RecipeRecommendationItem> appendFallbackRecommendations(String slotType,
                                                                         int limit,
                                                                         List<RecipeRecommendationItem> snapshotItems) {
        if (snapshotItems == null || snapshotItems.isEmpty()) {
            return loadFallbackOrEmpty(slotType, limit);
        }
        if (!fallbackEnabled) {
            return snapshotItems.stream()
                    .limit(limit)
                    .toList();
        }

        Map<Long, RecipeRecommendationItem> merged = new LinkedHashMap<>();
        for (RecipeRecommendationItem item : snapshotItems) {
            if (item == null || item.recipeId() == null) {
                continue;
            }
            merged.putIfAbsent(item.recipeId(), item);
        }
        if (merged.size() >= limit) {
            return merged.values().stream()
                    .limit(limit)
                    .toList();
        }

        List<RecipeRecommendationItem> fallbackItems = loadFallbackRecommendations(slotType, limit - merged.size(), merged.keySet());
        for (RecipeRecommendationItem item : fallbackItems) {
            if (item == null || item.recipeId() == null) {
                continue;
            }
            merged.putIfAbsent(item.recipeId(), item);
            if (merged.size() >= limit) {
                break;
            }
        }
        return merged.values().stream()
                .limit(limit)
                .toList();
    }

    @Transactional
    public void replaceSlot(LocalDate slotDate,
                            String slotType,
                            List<RecommendationCandidate> candidates,
                            LocalDateTime generatedAt,
                            LocalDateTime expiresAt) {
        String normalizedSlot = normalizeSlotType(slotType);
        snapshotRepository.deleteBySlot(slotDate, normalizedSlot);

        if (candidates == null || candidates.isEmpty()) {
            return;
        }

        List<RecipeRecoSnapshot> rows = new ArrayList<>();
        int rank = 1;
        for (RecommendationCandidate candidate : candidates) {
            if (candidate == null || candidate.recipeId() == null) {
                continue;
            }
            rows.add(RecipeRecoSnapshot.builder()
                    .slotDate(slotDate)
                    .slotType(normalizedSlot)
                    .rankNo(rank)
                    .recipeId(candidate.recipeId())
                    .score(candidate.score() == null ? BigDecimal.ZERO : candidate.score())
                    .generatedAt(generatedAt)
                    .expiresAt(expiresAt)
                    .build());
            rank++;
        }

        if (!rows.isEmpty()) {
            snapshotRepository.saveAll(rows);
        }
    }

    @Transactional
    public int deleteExpiredSnapshots() {
        return snapshotRepository.deleteExpired(LocalDateTime.now());
    }

    private String normalizeSlotType(String slotType) {
        if (slotType == null) {
            return SLOT_LUNCH;
        }
        String trimmed = slotType.trim().toUpperCase();
        if (SLOT_DINNER.equals(trimmed)) {
            return SLOT_DINNER;
        }
        return SLOT_LUNCH;
    }

    private int resolveCandidatePoolSize(int limit) {
        if (limit <= 0) {
            return 1;
        }
        if (limit > Integer.MAX_VALUE / FALLBACK_CANDIDATE_POOL_MULTIPLIER) {
            return Integer.MAX_VALUE;
        }
        return Math.max(limit, limit * FALLBACK_CANDIDATE_POOL_MULTIPLIER);
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
            String normalizedKeyword = normalizeText(keyword);
            if (normalizedKeyword.isEmpty()) {
                continue;
            }
            if (recipeName.contains(normalizedKeyword)
                    || typeName.contains(normalizedKeyword)
                    || nationName.contains(normalizedKeyword)
                    || ingredientCode.contains(normalizedKeyword)) {
                matches++;
            }
        }
        return matches;
    }

    private List<String> parseKeywords(String rawKeywords, String defaults) {
        String source = (rawKeywords == null || rawKeywords.isBlank()) ? defaults : rawKeywords;
        return Arrays.stream(source.split(","))
                .map(String::trim)
                .filter(keyword -> !keyword.isEmpty())
                .toList();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private record SlotAdjustedRecipe(Recipe_INFO recipe,
                                      BigDecimal adjustedScore,
                                      BigDecimal baseScore,
                                      int baseOrder) {
    }

    public record RecommendationCandidate(Long recipeId,
                                          BigDecimal score) {
    }

    public record RecipeRecommendationItem(Long recipeId,
                                           String recipeName,
                                           String imageUrl,
                                           BigDecimal score) {
    }

    private record ValidRecipeScoreBatch(List<Long> orderedIds,
                                         Map<Long, BigDecimal> scoreByRecipeId,
                                         Map<Long, Recipe_INFO> recipeById) {
    }
}
