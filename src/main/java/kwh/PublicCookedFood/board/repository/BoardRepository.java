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

    Page<Board> findByTitleContainingAndStateOrderByRegTimeDesc(String search, String state, Pageable pageable);

    Page<Board> findAllByStateOrderByRegTimeDesc(String state, Pageable pageable);

    @Modifying
    @Query("update Board p set p.state = '0' where p.id = :id")
    void deleteBoard(@Param("id") Long id);

}
