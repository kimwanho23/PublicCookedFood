package kwh.PublicCookedFood.board.repository;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardScrap;
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
public interface BoardScrapRepository extends JpaRepository<BoardScrap, Long> {

    boolean existsByBoardIdAndUserId(Long boardId, Long userId);

    long countByBoardId(Long boardId);

    @Modifying
    @Query("DELETE FROM BoardScrap bs WHERE bs.board.id = :boardId AND bs.user.id = :userId")
    int deleteByBoardIdAndUserId(@Param("boardId") Long boardId, @Param("userId") Long userId);

    @Query("SELECT b FROM BoardScrap bs " +
            "JOIN bs.board b " +
            "JOIN FETCH b.user " +
            "LEFT JOIN FETCH b.section " +
            "WHERE bs.user.id = :userId AND b.state = :state AND b.hiddenByReport = false " +
            "AND (:excludeBlocked = false OR b.user.id NOT IN :blockedUserIds) " +
            "ORDER BY bs.regTime DESC")
    List<Board> findScrappedBoardsByUserIdAndBoardState(@Param("userId") Long userId,
                                                         @Param("state") SoftDeleteState state,
                                                         @Param("excludeBlocked") boolean excludeBlocked,
                                                         @Param("blockedUserIds") Collection<Long> blockedUserIds);

    @Query(
            value = "SELECT b FROM BoardScrap bs " +
                    "JOIN bs.board b " +
                    "JOIN FETCH b.user " +
                    "LEFT JOIN FETCH b.section " +
                    "WHERE bs.user.id = :userId AND b.state = :state AND b.hiddenByReport = false " +
                    "AND (:excludeBlocked = false OR b.user.id NOT IN :blockedUserIds) " +
                    "ORDER BY bs.regTime DESC",
            countQuery = "SELECT COUNT(bs) FROM BoardScrap bs " +
                    "JOIN bs.board b " +
                    "WHERE bs.user.id = :userId AND b.state = :state AND b.hiddenByReport = false " +
                    "AND (:excludeBlocked = false OR b.user.id NOT IN :blockedUserIds)"
    )
    Page<Board> findScrappedBoardsPageByUserIdAndBoardState(@Param("userId") Long userId,
                                                             @Param("state") SoftDeleteState state,
                                                             @Param("excludeBlocked") boolean excludeBlocked,
                                                             @Param("blockedUserIds") Collection<Long> blockedUserIds,
                                                             Pageable pageable);
}
