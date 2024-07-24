package kwh.PublicCookedFood.user.repository;

import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.user.domain.Bookmark;
import kwh.PublicCookedFood.user.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

   List<Bookmark> findByUser(Users user);

   void deleteByUserAndRecipeID(Users user, Recipe_INFO recipeID);
   boolean existsByUserAndRecipeID(Users user, Recipe_INFO recipeID);
}
