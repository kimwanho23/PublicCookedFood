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

    @Query(
            value = "SELECT c FROM Comments c " +
                    "JOIN FETCH c.user " +
                    "WHERE c.board.id = :postId AND c.parent IS NULL " +
                    "AND (c.state = :activeState OR c.state = :deletedState) " +
                    "AND (:excludeBlocked = false OR c.user.id NOT IN :blockedUserIds) " +
                    "ORDER BY c.regTime ASC",
            countQuery = "SELECT COUNT(c) FROM Comments c " +
                    "WHERE c.board.id = :postId AND c.parent IS NULL " +
                    "AND (c.state = :activeState OR c.state = :deletedState) " +
                    "AND (:excludeBlocked = false OR c.user.id NOT IN :blockedUserIds)"
    )
    Page<Comments> findParentCommentsWithUserByBoardIdOrderByRegTimeAsc(@Param("postId") Long postId,
                                                                         @Param("activeState") SoftDeleteState activeState,
                                                                         @Param("deletedState") SoftDeleteState deletedState,
                                                                         @Param("excludeBlocked") boolean excludeBlocked,
                                                                         @Param("blockedUserIds") Collection<Long> blockedUserIds,
                                                                         Pageable pageable);

    @Query("SELECT c FROM Comments c " +
            "WHERE c.board.id = :postId AND c.parent IS NULL " +
            "AND (c.state = :activeState OR c.state = :deletedState) " +
            "AND (:excludeBlocked = false OR c.user.id NOT IN :blockedUserIds)")
    List<Comments> findParentCommentsByBoardId(@Param("postId") Long postId,
                                               @Param("activeState") SoftDeleteState activeState,
                                               @Param("deletedState") SoftDeleteState deletedState,
                                               @Param("excludeBlocked") boolean excludeBlocked,
                                               @Param("blockedUserIds") Collection<Long> blockedUserIds);

    Long countByBoardIdAndState(Long boardId, SoftDeleteState state);

    @Query("SELECT COUNT(c) FROM Comments c " +
            "WHERE c.board.id = :boardId AND c.state = :state AND c.user.id NOT IN :excludedUserIds")
    Long countByBoardIdAndStateAndUserIdNotIn(@Param("boardId") Long boardId,
                                              @Param("state") SoftDeleteState state,
                                              @Param("excludedUserIds") Collection<Long> excludedUserIds);

    Long countByUserIdAndState(Long userId, SoftDeleteState state);

    @Query("SELECT c FROM Comments c " +
            "JOIN FETCH c.user " +
            "JOIN FETCH c.parent " +
            "WHERE c.board.id = :postId AND c.parent IS NOT NULL " +
            "ORDER BY c.regTime ASC")
    List<Comments> findRepliesWithUserAndParentByBoardIdOrderByRegTimeAsc(@Param("postId") Long postId);

    @Query(
            value = "SELECT c FROM Comments c " +
                    "JOIN FETCH c.board b " +
                    "JOIN FETCH b.user " +
                    "LEFT JOIN FETCH b.section " +
                    "WHERE c.user.id = :userId " +
                    "AND c.state = :commentState " +
                    "AND b.state = :boardState " +
                    "AND b.hiddenByReport = false " +
                    "AND (:excludeBlocked = false OR b.user.id NOT IN :blockedUserIds) " +
                    "ORDER BY c.regTime DESC",
            countQuery = "SELECT COUNT(c) FROM Comments c " +
                    "JOIN c.board b " +
                    "WHERE c.user.id = :userId " +
                    "AND c.state = :commentState " +
                    "AND b.state = :boardState " +
                    "AND b.hiddenByReport = false " +
                    "AND (:excludeBlocked = false OR b.user.id NOT IN :blockedUserIds)"
    )
    Page<Comments> findUserCommentsWithBoard(@Param("userId") Long userId,
                                             @Param("commentState") SoftDeleteState commentState,
                                             @Param("boardState") SoftDeleteState boardState,
                                             @Param("excludeBlocked") boolean excludeBlocked,
                                             @Param("blockedUserIds") Collection<Long> blockedUserIds,
                                             Pageable pageable);

    @Modifying
    @Query("update Comments c set c.state = :state where c.id = :id")
    void updateState(@Param("id") Long id, @Param("state") SoftDeleteState state);

}
