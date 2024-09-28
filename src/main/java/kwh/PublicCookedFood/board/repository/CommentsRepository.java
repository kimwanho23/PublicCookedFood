package kwh.PublicCookedFood.board.repository;

import kwh.PublicCookedFood.board.domain.Comments;
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

    Page<Comments> findByBoardIdAndParentIsNullOrderByRegTimeAsc(Long postId, Pageable pageable);

    Long countByBoardIdAndState(Long boardId, String state);

    @Modifying
    @Query("update Comments p set p.state = '0' where p.id = :id")
    void deleteComment(@Param("id") Long id);

}
