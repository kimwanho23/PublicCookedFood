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
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeRecoSnapshotService {

    public static final String SLOT_LUNCH = "LUNCH";
    public static final String SLOT_DINNER = "DINNER";

    private final RecipeRecoSnapshotRepository snapshotRepository;
    private final Recipe_INFO_Repository recipeInfoRepository;
    private final RecipeStatsRepository recipeStatsRepository;

    @Value("${app.popular.recipe.snapshot.fallback-enabled:true}")
    private boolean fallbackEnabled;

    @Transactional(readOnly = true)
    public List<RecipeRecommendationItem> loadRecommendations(String slotType, int limit) {
        String normalizedSlot = normalizeSlotType(slotType);
        if (limit <= 0) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        List<RecipeRecoSnapshot> snapshotRows;
        try {
            snapshotRows = snapshotRepository.findActiveBySlot(
                    today,
                    normalizedSlot,
                    now,
                    Pageable.ofSize(limit)
            );
        } catch (RuntimeException e) {
            log.warn("Failed to load recipe recommendation snapshot. slotType={}, limit={}",
                    normalizedSlot, limit, e);
            return loadFallbackOrEmpty(limit);
        }
        if (snapshotRows.isEmpty()) {
            return loadFallbackOrEmpty(limit);
        }

        LinkedHashSet<Long> orderedIdSet = snapshotRows.stream()
                .map(RecipeRecoSnapshot::getRecipeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (orderedIdSet.isEmpty()) {
            return List.of();
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
        return orderedItems;
    }

    private List<RecipeRecommendationItem> loadFallbackOrEmpty(int limit) {
        if (!fallbackEnabled) {
            return List.of();
        }
        return loadFallbackRecommendations(limit);
    }

    private List<RecipeRecommendationItem> loadFallbackRecommendations(int limit) {
        int fetchSize = Math.max(limit, limit * 3);
        List<RecipeStatsRepository.RecipeScoreProjection> ranked =
                recipeStatsRepository.findTopRecipeScoresForSnapshot(PageRequest.of(0, fetchSize));
        if (ranked == null || ranked.isEmpty()) {
            return List.of();
        }

        List<Long> orderedIds = new ArrayList<>();
        Map<Long, BigDecimal> scoreByRecipeId = new LinkedHashMap<>();
        for (RecipeStatsRepository.RecipeScoreProjection row : ranked) {
            if (row == null || row.getRecipeId() == null) {
                continue;
            }
            Long recipeId = row.getRecipeId();
            if (scoreByRecipeId.containsKey(recipeId)) {
                continue;
            }
            orderedIds.add(recipeId);
            scoreByRecipeId.put(recipeId, row.getScore() == null ? BigDecimal.ZERO : row.getScore());
        }
        if (orderedIds.isEmpty()) {
            return List.of();
        }

        List<Recipe_INFO> recipes = recipeInfoRepository.findAllByRecipeIDIn(orderedIds);
        Map<Long, Recipe_INFO> recipeById = new LinkedHashMap<>();
        for (Recipe_INFO recipe : recipes) {
            if (recipe == null || recipe.getRecipeID() == null) {
                continue;
            }
            recipeById.putIfAbsent(recipe.getRecipeID(), recipe);
        }

        List<RecipeRecommendationItem> items = new ArrayList<>();
        for (Long recipeId : orderedIds) {
            Recipe_INFO recipe = recipeById.get(recipeId);
            if (recipe == null) {
                continue;
            }
            items.add(new RecipeRecommendationItem(
                    recipe.getRecipeID(),
                    recipe.getRecipeNMKO(),
                    recipe.getImgURL(),
                    scoreByRecipeId.getOrDefault(recipeId, BigDecimal.ZERO)
            ));
            if (items.size() >= limit) {
                break;
            }
        }
        return items;
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

    public record RecommendationCandidate(Long recipeId,
                                          BigDecimal score) {
    }

    public record RecipeRecommendationItem(Long recipeId,
                                           String recipeName,
                                           String imageUrl,
                                           BigDecimal score) {
    }
}
