package kwh.PublicCookedFood.metrics.view;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BoardStatsRepository extends JpaRepository<BoardStats, Long> {

    @Query("SELECT s.totalViews FROM BoardStats s WHERE s.boardId = :boardId")
    Optional<Long> findTotalViewsByBoardId(@Param("boardId") Long boardId);

    @Query("SELECT s.boardId AS boardId, s.totalViews AS totalViews FROM BoardStats s WHERE s.boardId IN :boardIds")
    List<BoardViewCountProjection> findViewCountsByBoardIdIn(@Param("boardIds") Collection<Long> boardIds);

    @Modifying
    @Query("UPDATE BoardStats s SET s.totalViews = s.totalViews + :delta, s.updatedAt = CURRENT_TIMESTAMP WHERE s.boardId = :boardId")
    int addViews(@Param("boardId") Long boardId, @Param("delta") Long delta);

    @Modifying
    @Query("UPDATE BoardStats s SET s.totalViews = :totalViews, s.updatedAt = CURRENT_TIMESTAMP WHERE s.boardId = :boardId")
    int updateTotalViews(@Param("boardId") Long boardId, @Param("totalViews") Long totalViews);

    interface BoardViewCountProjection {
        Long getBoardId();

        Long getTotalViews();
    }
}
