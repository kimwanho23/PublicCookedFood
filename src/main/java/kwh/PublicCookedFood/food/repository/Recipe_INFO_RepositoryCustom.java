package kwh.PublicCookedFood.food.repository;

import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface Recipe_INFO_RepositoryCustom {

    Page<Recipe_INFO> findRecipesByConditions(@Param("type") String type,
                                              @Param("nation") String nation,
                                              @Param("ingredient") String ingredient,
                                              @Param("keyword") String keyword,
                                              @Param("search") String search,
                                              Pageable pageable);
}
