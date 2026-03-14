package kwh.PublicCookedFood.userrecipe.repository;

import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipeStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRecipeStepRepository extends JpaRepository<UserRecipeStep, Long> {

    List<UserRecipeStep> findAllByRecipeIdOrderByStepNoAsc(Long recipeId);

    @Query("SELECT CASE WHEN COUNT(step) > 0 THEN true ELSE false END " +
            "FROM UserRecipeStep step JOIN step.recipe recipe " +
            "WHERE step.imageUrl = :imageUrl AND recipe.state = :state")
    boolean existsByImageUrlAndRecipeState(@Param("imageUrl") String imageUrl,
                                           @Param("state") SoftDeleteState state);
}
