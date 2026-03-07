package kwh.PublicCookedFood.metrics.view;

import kwh.PublicCookedFood.metrics.reco.RecipeScoreEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecipeViewCounterServiceImpl implements RecipeViewCounterService {

    private final RecipeStatsRepository recipeStatsRepository;
    private final RecipeScoreEventPublisher recipeScoreEventPublisher;

    @Override
    @Transactional
    public long increaseRecipeViewAndGet(Long recipeId) {
        if (recipeId == null) {
            throw new IllegalArgumentException("레시피 ID는 필수입니다.");
        }

        int updated = recipeStatsRepository.addViews(recipeId, 1L);
        if (updated <= 0) {
            try {
                recipeStatsRepository.save(RecipeStats.initialize(recipeId, 1L));
            } catch (DataIntegrityViolationException e) {
                recipeStatsRepository.addViews(recipeId, 1L);
            }
        }
        long viewCount = recipeStatsRepository.findTotalViewsByRecipeId(recipeId).orElse(0L);
        recipeScoreEventPublisher.publishRecalculateRequest(recipeId, "VIEW_INCREMENT");
        return viewCount;
    }

    @Override
    @Transactional(readOnly = true)
    public long getRecipeViewCount(Long recipeId) {
        if (recipeId == null) {
            throw new IllegalArgumentException("레시피 ID는 필수입니다.");
        }
        return recipeStatsRepository.findTotalViewsByRecipeId(recipeId).orElse(0L);
    }
}
