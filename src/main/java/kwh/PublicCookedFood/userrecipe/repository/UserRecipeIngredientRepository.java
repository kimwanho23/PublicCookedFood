package kwh.PublicCookedFood.userrecipe.repository;

import kwh.PublicCookedFood.userrecipe.domain.UserRecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRecipeIngredientRepository extends JpaRepository<UserRecipeIngredient, Long> {

    List<UserRecipeIngredient> findAllByRecipeId(Long recipeId);
}
