package kwh.PublicCookedFood.userrecipe.repository;

import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRecipeRepository extends JpaRepository<UserRecipe, Long> {

    @EntityGraph(attributePaths = {"account"})
    Page<UserRecipe> findByStateOrderByRegTimeDesc(SoftDeleteState state, Pageable pageable);

    Optional<UserRecipe> findByIdAndState(Long recipeId, SoftDeleteState state);

    boolean existsByThumbnailUrlAndState(String thumbnailUrl, SoftDeleteState state);

    @Query("SELECT ur FROM UserRecipe ur " +
            "JOIN FETCH ur.account " +
            "WHERE ur.id = :recipeId AND ur.state = :state")
    Optional<UserRecipe> findByIdWithAccountAndState(@Param("recipeId") Long recipeId,
                                                     @Param("state") SoftDeleteState state);
}
