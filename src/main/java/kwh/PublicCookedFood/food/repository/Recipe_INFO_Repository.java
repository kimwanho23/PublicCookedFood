package kwh.PublicCookedFood.food.repository;

import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface Recipe_INFO_Repository extends JpaRepository<Recipe_INFO, Long>, JpaSpecificationExecutor<Recipe_INFO> {
    Recipe_INFO findByRecipeID(Long RecipeID);

    @Query("SELECT DISTINCT r.tyNM FROM Recipe_INFO r")
    List<String> findDistinctTyNM(); //음식별 카테고리

    @Query("SELECT DISTINCT r.irdntCODE FROM Recipe_INFO r WHERE r.irdntCODE IS NOT NULL AND TRIM(r.irdntCODE) <> ''")
    List<String> findDistinctByIrdntCODEIsNotNull(); //재료별 카테고리

    @Query("SELECT DISTINCT r.nationNM FROM Recipe_INFO r")
    List<String> findDistinctNationNM(); // 유형별 카테고리

    List<Recipe_INFO> findByRecipeNMKOContaining(String keyword);

}
