package kwh.PublicCookedFood.food.repository;

import kwh.PublicCookedFood.food.entity.Recipe_IRDNT;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

public interface Recipe_IRDNT_Repository extends JpaRepository<Recipe_IRDNT, Long> {

    List<Recipe_IRDNT> findAllByRecipeIDOrderByIrdntTYCODEAsc(Long recipeId);
}
