package kwh.PublicCookedFood.user.repository;

import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.user.domain.Bookmark;
import kwh.PublicCookedFood.user.domain.Users;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

   List<Bookmark> findByUser(Users user);

   @Query("SELECT DISTINCT b FROM Bookmark b " +
           "JOIN FETCH b.recipeID " +
           "JOIN FETCH b.user " +
           "WHERE b.user.email = :email")
   List<Bookmark> findBookmarksByUserEmail(@Param("email") String email);

   void deleteByUserAndRecipeID(Users user, Recipe_INFO recipeID);

   boolean existsByUserAndRecipeID(Users user, Recipe_INFO recipeID);

   Optional<Bookmark> findByUserAndRecipeID(Users user, Recipe_INFO recipeID);

}
