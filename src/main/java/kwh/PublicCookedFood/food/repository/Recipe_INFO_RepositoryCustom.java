package kwh.PublicCookedFood.food.repository;

import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface Recipe_INFO_RepositoryCustom {

    Page<Recipe_INFO> findRecipesByConditions(@Param("type") List<String> type,
                                              @Param("nation") List<String> nation,
                                              @Param("ingredient") List<String> ingredient,
                                              @Param("keyword") String keyword,
                                              @Param("search") String search,
                                              Pageable pageable);
}
