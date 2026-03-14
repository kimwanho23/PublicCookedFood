package kwh.PublicCookedFood.userrecipe.repository;

import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.userrecipe.domain.UserRecipeComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UserRecipeCommentRepository extends JpaRepository<UserRecipeComment, Long> {

    @Query("SELECT c FROM UserRecipeComment c " +
            "JOIN FETCH c.account " +
            "WHERE c.recipe.id = :recipeId AND c.parent IS NULL " +
            "AND (c.state = :activeState OR c.state = :deletedState) " +
            "AND (:excludeBlocked = false OR c.account.id NOT IN :blockedAccountIds) " +
            "ORDER BY c.regTime ASC")
    List<UserRecipeComment> findParentCommentsWithAccountByRecipeIdOrderByRegTimeAsc(@Param("recipeId") Long recipeId,
                                                                                     @Param("activeState") SoftDeleteState activeState,
                                                                                     @Param("deletedState") SoftDeleteState deletedState,
                                                                                     @Param("excludeBlocked") boolean excludeBlocked,
                                                                                     @Param("blockedAccountIds") Collection<Long> blockedAccountIds);

    @Query(
            value = "SELECT c FROM UserRecipeComment c " +
                    "JOIN FETCH c.account " +
                    "WHERE c.recipe.id = :recipeId AND c.parent IS NULL " +
                    "AND (c.state = :activeState OR c.state = :deletedState) " +
                    "AND (:excludeBlocked = false OR c.account.id NOT IN :blockedAccountIds) " +
                    "ORDER BY c.regTime ASC",
            countQuery = "SELECT COUNT(c) FROM UserRecipeComment c " +
                    "WHERE c.recipe.id = :recipeId AND c.parent IS NULL " +
                    "AND (c.state = :activeState OR c.state = :deletedState) " +
                    "AND (:excludeBlocked = false OR c.account.id NOT IN :blockedAccountIds)"
    )
    Page<UserRecipeComment> findParentCommentsPageWithAccountByRecipeIdOrderByRegTimeAsc(@Param("recipeId") Long recipeId,
                                                                                          @Param("activeState") SoftDeleteState activeState,
                                                                                          @Param("deletedState") SoftDeleteState deletedState,
                                                                                          @Param("excludeBlocked") boolean excludeBlocked,
                                                                                          @Param("blockedAccountIds") Collection<Long> blockedAccountIds,
                                                                                          Pageable pageable);

    Long countByRecipeIdAndState(Long recipeId, SoftDeleteState state);

    @Query("SELECT COUNT(c) FROM UserRecipeComment c " +
            "WHERE c.recipe.id = :recipeId AND c.state = :state AND c.account.id NOT IN :excludedAccountIds")
    Long countByRecipeIdAndStateAndAccountIdNotIn(@Param("recipeId") Long recipeId,
                                                  @Param("state") SoftDeleteState state,
                                                  @Param("excludedAccountIds") Collection<Long> excludedAccountIds);

    @Query("SELECT c FROM UserRecipeComment c " +
            "JOIN FETCH c.account " +
            "JOIN FETCH c.parent " +
            "WHERE c.recipe.id = :recipeId " +
            "AND c.parent IS NOT NULL " +
            "AND c.rootParentId IN :rootParentIds " +
            "AND (:excludeBlocked = false OR c.account.id NOT IN :blockedAccountIds) " +
            "ORDER BY c.commentPath ASC")
    List<UserRecipeComment> findRepliesWithAccountAndParentByRecipeIdAndRootParentIdInOrderByCommentPathAsc(@Param("recipeId") Long recipeId,
                                                                                                              @Param("rootParentIds") Collection<Long> rootParentIds,
                                                                                                              @Param("excludeBlocked") boolean excludeBlocked,
                                                                                                              @Param("blockedAccountIds") Collection<Long> blockedAccountIds);

    @Modifying
    @Query("update UserRecipeComment c set c.state = :state where c.id = :id")
    void updateState(@Param("id") Long id, @Param("state") SoftDeleteState state);
}
