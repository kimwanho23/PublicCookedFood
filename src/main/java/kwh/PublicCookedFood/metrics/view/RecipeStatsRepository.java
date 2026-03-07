package kwh.PublicCookedFood.metrics.view;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RecipeStatsRepository extends JpaRepository<RecipeStats, Long> {

    @Query("SELECT s.totalViews FROM RecipeStats s WHERE s.recipeId = :recipeId")
    Optional<Long> findTotalViewsByRecipeId(@Param("recipeId") Long recipeId);

    @Modifying
    @Query("UPDATE RecipeStats s SET s.totalViews = s.totalViews + :delta, s.updatedAt = CURRENT_TIMESTAMP WHERE s.recipeId = :recipeId")
    int addViews(@Param("recipeId") Long recipeId, @Param("delta") Long delta);

    @Modifying
    @Query("UPDATE RecipeStats s SET s.totalViews = :totalViews, s.updatedAt = CURRENT_TIMESTAMP WHERE s.recipeId = :recipeId")
    int updateTotalViews(@Param("recipeId") Long recipeId, @Param("totalViews") Long totalViews);

    @Modifying
    @Query("UPDATE RecipeStats s " +
            "SET s.totalBookmarks = CASE WHEN (s.totalBookmarks + :delta) < 0 THEN 0 ELSE (s.totalBookmarks + :delta) END, " +
            "s.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE s.recipeId = :recipeId")
    int addBookmarks(@Param("recipeId") Long recipeId, @Param("delta") Long delta);

    @Query("SELECT s.recipeId AS recipeId, s.score AS score FROM RecipeStats s " +
            "ORDER BY s.score DESC, s.totalViews DESC, s.totalBookmarks DESC, s.updatedAt DESC")
    List<RecipeScoreProjection> findTopRecipeScoresForSnapshot(Pageable pageable);

    List<RecipeStats> findByRecipeIdGreaterThanOrderByRecipeIdAsc(Long recipeId, Pageable pageable);

    interface RecipeScoreProjection {
        Long getRecipeId();

        BigDecimal getScore();
    }
}
