package kwh.PublicCookedFood.metrics.reco;

import kwh.PublicCookedFood.food.repository.RecipeReviewRepository;
import kwh.PublicCookedFood.metrics.view.RecipeStats;
import kwh.PublicCookedFood.metrics.view.RecipeStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecipeStatsScoreScheduler {

    private final RecipeStatsRepository recipeStatsRepository;
    private final RecipeReviewRepository recipeReviewRepository;
    private final RecipeStatsScoreService recipeStatsScoreService;

    @Value("${app.popular.recipe.score.enabled:false}")
    private boolean enabled;

    @Value("${app.popular.recipe.score.batch-size:500}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.popular.recipe.score.interval-ms:300000}")
    @Transactional
    public void recalculateRecipeScores() {
        if (!enabled) {
            return;
        }

        int normalizedBatchSize = Math.max(1, batchSize);
        LocalDateTime now = LocalDateTime.now();
        Map<Long, LocalDateTime> latestReviewTimeByRecipeId = loadLatestReviewTimes();

        long lastRecipeId = -1L;
        int touched = 0;
        while (true) {
            List<RecipeStats> batch = recipeStatsRepository.findByRecipeIdGreaterThanOrderByRecipeIdAsc(
                    lastRecipeId,
                    PageRequest.of(0, normalizedBatchSize)
            );
            if (batch.isEmpty()) {
                break;
            }

            for (RecipeStats recipeStats : batch) {
                Long recipeId = recipeStats.getRecipeId();
                if (recipeId == null) {
                    continue;
                }
                LocalDateTime latestReviewTime = latestReviewTimeByRecipeId.get(recipeId);
                BigDecimal score = recipeStatsScoreService.calculateScore(recipeStats, latestReviewTime, now);
                recipeStats.updateScore(score);
            }

            recipeStatsRepository.saveAll(batch);
            touched += batch.size();
            lastRecipeId = batch.get(batch.size() - 1).getRecipeId();

            if (batch.size() < normalizedBatchSize) {
                break;
            }
        }

        log.debug("Recalculated recipe scores. touched={}", touched);
    }

    private Map<Long, LocalDateTime> loadLatestReviewTimes() {
        List<RecipeReviewRepository.RecipeReviewRecencyProjection> projections =
                recipeReviewRepository.findLatestReviewTimeByRecipeId();
        Map<Long, LocalDateTime> result = new HashMap<>();
        if (projections == null) {
            return result;
        }
        for (RecipeReviewRepository.RecipeReviewRecencyProjection projection : projections) {
            if (projection == null || projection.getRecipeId() == null || projection.getLatestReviewTime() == null) {
                continue;
            }
            result.putIfAbsent(projection.getRecipeId(), projection.getLatestReviewTime());
        }
        return result;
    }
}
