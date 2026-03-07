package kwh.PublicCookedFood.metrics.view;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeStatsMutationService {

    private final RecipeStatsRepository recipeStatsRepository;

    @Transactional
    public void adjustBookmarkCount(Long recipeId, long delta) {
        if (recipeId == null || delta == 0L) {
            return;
        }

        int updated = recipeStatsRepository.addBookmarks(recipeId, delta);
        if (updated > 0) {
            return;
        }
        if (delta < 0L) {
            return;
        }

        try {
            recipeStatsRepository.save(RecipeStats.builder()
                    .recipeId(recipeId)
                    .totalViews(0L)
                    .totalLikes(0L)
                    .totalBookmarks(delta)
                    .score(BigDecimal.ZERO)
                    .updatedAt(LocalDateTime.now())
                    .build());
        } catch (DataIntegrityViolationException e) {
            recipeStatsRepository.addBookmarks(recipeId, delta);
        } catch (RuntimeException e) {
            log.warn("Failed to adjust recipe bookmark stats. recipeId={}, delta={}", recipeId, delta, e);
        }
    }
}
