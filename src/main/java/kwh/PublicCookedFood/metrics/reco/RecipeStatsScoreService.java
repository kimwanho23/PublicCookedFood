package kwh.PublicCookedFood.metrics.reco;

import kwh.PublicCookedFood.food.repository.RecipeReviewRepository;
import kwh.PublicCookedFood.metrics.view.RecipeStats;
import kwh.PublicCookedFood.metrics.view.RecipeStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeStatsScoreService {

    private final RecipeStatsRepository recipeStatsRepository;
    private final RecipeReviewRepository recipeReviewRepository;

    @Value("${app.popular.recipe.score.weight-views:1.00}")
    private BigDecimal viewsWeight;

    @Value("${app.popular.recipe.score.weight-likes:0.00}")
    private BigDecimal likesWeight;

    @Value("${app.popular.recipe.score.weight-bookmarks:1.30}")
    private BigDecimal bookmarksWeight;

    @Value("${app.popular.recipe.score.freshness-weight:0.70}")
    private BigDecimal freshnessWeight;

    @Value("${app.popular.recipe.score.freshness-lambda:0.03}")
    private BigDecimal freshnessLambda;

    @Transactional
    public void recalculateScore(Long recipeId) {
        if (recipeId == null) {
            return;
        }
        try {
            RecipeStats recipeStats = recipeStatsRepository.findById(recipeId).orElse(null);
            if (recipeStats == null) {
                return;
            }
            LocalDateTime latestReviewTime = recipeReviewRepository.findLatestReviewTimeByRecipeId(recipeId)
                    .orElse(null);
            BigDecimal score = calculateScore(recipeStats, latestReviewTime, LocalDateTime.now());
            recipeStats.updateScore(score);
            recipeStatsRepository.save(recipeStats);
        } catch (RuntimeException e) {
            log.warn("Failed to recalculate recipe score immediately. recipeId={}", recipeId, e);
        }
    }

    public BigDecimal calculateScore(RecipeStats recipeStats,
                                     LocalDateTime latestReviewTime,
                                     LocalDateTime now) {
        if (recipeStats == null) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        long totalBookmarks = Math.max(0L, recipeStats.getTotalBookmarks() == null ? 0L : recipeStats.getTotalBookmarks());
        BigDecimal viewsTerm = weightedLog(recipeStats.getTotalViews(), viewsWeight);
        BigDecimal likesTerm = weightedLog(recipeStats.getTotalLikes(), likesWeight);
        BigDecimal bookmarksTerm = weightedLog(totalBookmarks, bookmarksWeight);
        BigDecimal freshnessBonus = calculateFreshnessBonus(latestReviewTime, now);

        return viewsTerm
                .add(likesTerm)
                .add(bookmarksTerm)
                .add(freshnessBonus)
                .setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal weightedLog(Long count, BigDecimal weight) {
        long normalizedCount = Math.max(0L, count == null ? 0L : count);
        BigDecimal normalizedWeight = weight == null ? BigDecimal.ZERO : weight;
        if (normalizedWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        double logValue = Math.log1p(normalizedCount);
        return normalizedWeight.multiply(BigDecimal.valueOf(logValue));
    }

    private BigDecimal calculateFreshnessBonus(LocalDateTime latestReviewTime, LocalDateTime now) {
        BigDecimal normalizedFreshnessWeight = freshnessWeight == null ? BigDecimal.ZERO : freshnessWeight;
        if (normalizedFreshnessWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (latestReviewTime == null || now == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal normalizedLambda = freshnessLambda == null ? BigDecimal.ZERO : freshnessLambda.max(BigDecimal.ZERO);
        double ageHours = Math.max(0D, Duration.between(latestReviewTime, now).toMinutes() / 60.0D);
        double decay = Math.exp(-normalizedLambda.doubleValue() * ageHours);
        return normalizedFreshnessWeight.multiply(BigDecimal.valueOf(decay));
    }
}
