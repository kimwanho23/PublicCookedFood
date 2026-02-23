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

import java.util.List;

@Repository
public interface CommentsRepository extends JpaRepository<Comments, Long> {

    @Query(
            value = "SELECT c FROM Comments c " +
                    "JOIN FETCH c.user " +
                    "WHERE c.board.id = :postId AND c.parent IS NULL " +
                    "ORDER BY c.regTime ASC",
            countQuery = "SELECT COUNT(c) FROM Comments c " +
                    "WHERE c.board.id = :postId AND c.parent IS NULL"
    )
    Page<Comments> findParentCommentsWithUserByBoardIdOrderByRegTimeAsc(@Param("postId") Long postId, Pageable pageable);

    Long countByBoardIdAndState(Long boardId, SoftDeleteState state);

    @Query("SELECT c FROM Comments c " +
            "JOIN FETCH c.user " +
            "JOIN FETCH c.parent " +
            "WHERE c.board.id = :postId AND c.parent IS NOT NULL " +
            "ORDER BY c.regTime ASC")
    List<Comments> findRepliesWithUserAndParentByBoardIdOrderByRegTimeAsc(@Param("postId") Long postId);

    @Modifying
    @Query("update Comments c set c.state = :state where c.id = :id")
    void updateState(@Param("id") Long id, @Param("state") SoftDeleteState state);

}
