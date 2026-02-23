package kwh.PublicCookedFood.board.repository;

import kwh.PublicCookedFood.board.domain.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    @Modifying
    @Query("update Board p set p.views = p.views + 1 where p.id = :id")
    void updateViews(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Board b SET b.likeCount = (SELECT COUNT(l) FROM Likes l WHERE l.board.id = :boardId) WHERE b.id = :boardId")
    void updateLikes(@Param("boardId") Long boardId);

    @Modifying
    @Query("UPDATE Board b SET b.commentCount = :commentCount WHERE b.id = :postId and b.state = '1'")
    void updateCommentCount(@Param("postId") Long postId, @Param("commentCount") Long commentCount);

    @Query(
            value = "SELECT b FROM Board b JOIN FETCH b.user " +
                    "WHERE b.state = :state AND b.title LIKE CONCAT('%', :search, '%') " +
                    "ORDER BY b.regTime DESC",
            countQuery = "SELECT COUNT(b) FROM Board b " +
                    "WHERE b.state = :state AND b.title LIKE CONCAT('%', :search, '%')"
    )
    Page<Board> findByTitleContainingAndStateWithUser(@Param("search") String search,
                                                      @Param("state") String state,
                                                      Pageable pageable);

    @Query(
            value = "SELECT b FROM Board b JOIN FETCH b.user WHERE b.state = :state ORDER BY b.regTime DESC",
            countQuery = "SELECT COUNT(b) FROM Board b WHERE b.state = :state"
    )
    Page<Board> findAllByStateWithUser(@Param("state") String state, Pageable pageable);



    @Modifying
    @Query(value = "update Board p set p.state = '0' where p.id = :id", nativeQuery = true)
    void deleteBoardOption(@Param("id") Long id);

}
