package kwh.PublicCookedFood.board.repository;

import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CommentsRepository extends JpaRepository<Comments, Long> {

    @Query("SELECT c FROM Comments c " +
            "JOIN FETCH c.account " +
            "WHERE c.board.id = :postId AND c.parent IS NULL " +
            "AND (c.state = :activeState OR c.state = :deletedState) " +
            "AND (:excludeBlocked = false OR c.account.id NOT IN :blockedAccountIds) " +
            "ORDER BY c.regTime ASC")
    List<Comments> findParentCommentsWithAccountByBoardIdOrderByRegTimeAsc(@Param("postId") Long postId,
                                                                           @Param("activeState") SoftDeleteState activeState,
                                                                           @Param("deletedState") SoftDeleteState deletedState,
                                                                           @Param("excludeBlocked") boolean excludeBlocked,
                                                                           @Param("blockedAccountIds") Collection<Long> blockedAccountIds);

    @Query(
            value = "SELECT c FROM Comments c " +
                    "JOIN FETCH c.account " +
                    "WHERE c.board.id = :postId AND c.parent IS NULL " +
                    "AND (c.state = :activeState OR c.state = :deletedState) " +
                    "AND (:excludeBlocked = false OR c.account.id NOT IN :blockedAccountIds) " +
                    "ORDER BY c.regTime ASC",
            countQuery = "SELECT COUNT(c) FROM Comments c " +
                    "WHERE c.board.id = :postId AND c.parent IS NULL " +
                    "AND (c.state = :activeState OR c.state = :deletedState) " +
                    "AND (:excludeBlocked = false OR c.account.id NOT IN :blockedAccountIds)"
    )
    Page<Comments> findParentCommentsPageWithAccountByBoardIdOrderByRegTimeAsc(@Param("postId") Long postId,
                                                                                @Param("activeState") SoftDeleteState activeState,
                                                                                @Param("deletedState") SoftDeleteState deletedState,
                                                                                @Param("excludeBlocked") boolean excludeBlocked,
                                                                                @Param("blockedAccountIds") Collection<Long> blockedAccountIds,
                                                                                Pageable pageable);

    @Query("SELECT c FROM Comments c " +
            "WHERE c.board.id = :postId AND c.parent IS NULL " +
            "AND (c.state = :activeState OR c.state = :deletedState) " +
            "AND (:excludeBlocked = false OR c.account.id NOT IN :blockedAccountIds)")
    List<Comments> findParentCommentsByBoardId(@Param("postId") Long postId,
                                               @Param("activeState") SoftDeleteState activeState,
                                               @Param("deletedState") SoftDeleteState deletedState,
                                               @Param("excludeBlocked") boolean excludeBlocked,
                                               @Param("blockedAccountIds") Collection<Long> blockedAccountIds);

    Long countByBoardIdAndState(Long boardId, SoftDeleteState state);

    @Query("SELECT COUNT(c) FROM Comments c " +
            "WHERE c.board.id = :boardId AND c.state = :state AND c.account.id NOT IN :excludedAccountIds")
    Long countByBoardIdAndStateAndAccountIdNotIn(@Param("boardId") Long boardId,
                                                 @Param("state") SoftDeleteState state,
                                                 @Param("excludedAccountIds") Collection<Long> excludedAccountIds);

    Long countByAccountIdAndState(Long accountId, SoftDeleteState state);

    @Query("SELECT c FROM Comments c " +
            "JOIN FETCH c.account " +
            "JOIN FETCH c.parent " +
            "WHERE c.board.id = :postId " +
            "AND c.parent IS NOT NULL " +
            "AND c.rootParentId IN :rootParentIds " +
            "AND (:excludeBlocked = false OR c.account.id NOT IN :blockedAccountIds) " +
            "ORDER BY c.commentPath ASC")
    List<Comments> findRepliesWithAccountAndParentByBoardIdAndRootParentIdInOrderByCommentPathAsc(@Param("postId") Long postId,
                                                                                                   @Param("rootParentIds") Collection<Long> rootParentIds,
                                                                                                   @Param("excludeBlocked") boolean excludeBlocked,
                                                                                                   @Param("blockedAccountIds") Collection<Long> blockedAccountIds);

    @Query("SELECT c FROM Comments c " +
            "LEFT JOIN FETCH c.parent " +
            "JOIN FETCH c.board " +
            "LEFT JOIN FETCH c.account " +
            "WHERE c.id IN :commentIds")
    List<Comments> findWithBoardAndParentByIdIn(@Param("commentIds") Collection<Long> commentIds);

    @Query(
            value = "SELECT c FROM Comments c " +
                    "JOIN FETCH c.board b " +
                    "JOIN FETCH b.account " +
                    "LEFT JOIN FETCH b.section " +
                    "WHERE c.account.id = :accountId " +
                    "AND c.state = :commentState " +
                    "AND b.state = :boardState " +
                    "AND b.hiddenByReport = false " +
                    "AND (:excludeBlocked = false OR b.account.id NOT IN :blockedAccountIds) " +
                    "ORDER BY c.regTime DESC",
            countQuery = "SELECT COUNT(c) FROM Comments c " +
                    "JOIN c.board b " +
                    "WHERE c.account.id = :accountId " +
                    "AND c.state = :commentState " +
                    "AND b.state = :boardState " +
                    "AND b.hiddenByReport = false " +
                    "AND (:excludeBlocked = false OR b.account.id NOT IN :blockedAccountIds)"
    )
    Page<Comments> findAccountCommentsWithBoard(@Param("accountId") Long accountId,
                                             @Param("commentState") SoftDeleteState commentState,
                                             @Param("boardState") SoftDeleteState boardState,
                                             @Param("excludeBlocked") boolean excludeBlocked,
                                             @Param("blockedAccountIds") Collection<Long> blockedAccountIds,
                                             Pageable pageable);

    @Modifying
    @Query("update Comments c set c.state = :state where c.id = :id")
    void updateState(@Param("id") Long id, @Param("state") SoftDeleteState state);

}
