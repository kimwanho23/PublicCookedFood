package kwh.PublicCookedFood.food.repository;

import kwh.PublicCookedFood.food.entity.RecipeReview;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.account.domain.Account;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RecipeReviewRepository extends JpaRepository<RecipeReview, Long> {

    Optional<RecipeReview> findByRecipeAndAccount(Recipe_INFO recipe, Account account);

    List<RecipeReview> findTop20ByRecipeOrderByRegTimeDesc(Recipe_INFO recipe);

    long countByRecipe(Recipe_INFO recipe);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM RecipeReview r WHERE r.recipe = :recipe")
    Double findAverageRatingByRecipe(@Param("recipe") Recipe_INFO recipe);

    @Query("SELECT r.recipe as recipe, COUNT(r.id) as reviewCount, AVG(r.rating) as avgRating " +
            "FROM RecipeReview r " +
            "WHERE r.regTime >= :since " +
            "GROUP BY r.recipe " +
            "ORDER BY COUNT(r.id) DESC, AVG(r.rating) DESC, MAX(r.regTime) DESC")
    List<RecipeReviewRankingProjection> findTopReviewRankingsSince(@Param("since") LocalDateTime since, Pageable pageable);

    @Query("SELECT r.recipe.recipeID AS recipeId, MAX(r.regTime) AS latestReviewTime " +
            "FROM RecipeReview r " +
            "GROUP BY r.recipe.recipeID")
    List<RecipeReviewRecencyProjection> findLatestReviewTimeByRecipeId();

    @Query("SELECT MAX(r.regTime) FROM RecipeReview r WHERE r.recipe.recipeID = :recipeId")
    Optional<LocalDateTime> findLatestReviewTimeByRecipeId(@Param("recipeId") Long recipeId);

    interface RecipeReviewRankingProjection {
        Recipe_INFO getRecipe();

        Long getReviewCount();

        Double getAvgRating();
    }

    interface RecipeReviewRecencyProjection {
        Long getRecipeId();

        LocalDateTime getLatestReviewTime();
    }
}
