package kwh.PublicCookedFood.account.repository;

import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.account.domain.Bookmark;
import kwh.PublicCookedFood.account.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

   @Query("SELECT DISTINCT b FROM Bookmark b " +
           "JOIN FETCH b.recipeID " +
           "WHERE b.account.id = :accountId")
   List<Bookmark> findBookmarksByAccountId(@Param("accountId") Long accountId);

   long deleteByAccountAndRecipeID(Account account, Recipe_INFO recipeID);

   boolean existsByAccountAndRecipeID(Account account, Recipe_INFO recipeID);

   Optional<Bookmark> findByAccountAndRecipeID(Account account, Recipe_INFO recipeID);

}

