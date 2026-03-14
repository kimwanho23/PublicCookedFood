package kwh.PublicCookedFood.userrecipe.repository;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipe;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipeReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRecipeReviewRepository extends JpaRepository<UserRecipeReview, Long> {

    Optional<UserRecipeReview> findByRecipeAndAccount(UserRecipe recipe, Account account);

    List<UserRecipeReview> findTop20ByRecipeOrderByRegTimeDesc(UserRecipe recipe);

    long countByRecipe(UserRecipe recipe);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM UserRecipeReview r WHERE r.recipe = :recipe")
    Double findAverageRatingByRecipe(@Param("recipe") UserRecipe recipe);

    @Query("SELECT CASE WHEN COUNT(review) > 0 THEN true ELSE false END " +
            "FROM UserRecipeReview review JOIN review.recipe recipe " +
            "WHERE review.imageUrl = :imageUrl AND recipe.state = :state")
    boolean existsByImageUrlAndRecipeState(@Param("imageUrl") String imageUrl,
                                           @Param("state") SoftDeleteState state);

    @Query("SELECT review.imageUrl FROM UserRecipeReview review " +
            "WHERE review.recipe.id = :recipeId AND review.imageUrl IS NOT NULL AND review.imageUrl <> ''")
    List<String> findImageUrlsByRecipeId(@Param("recipeId") Long recipeId);
}
