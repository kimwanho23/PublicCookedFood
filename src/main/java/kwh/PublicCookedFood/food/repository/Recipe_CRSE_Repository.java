package kwh.PublicCookedFood.food.repository;

import kwh.PublicCookedFood.food.entity.Recipe_CRSE;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Recipe_CRSE_Repository extends JpaRepository<Recipe_CRSE, Long> {
    List<Recipe_CRSE> findAllByRecipeIDOrderByCookingNOAsc(Long RecipeID);


}
