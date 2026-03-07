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

    boolean existsByBoardIdAndAccountId(Long boardId, Long accountId);

    long countByBoardId(Long boardId);

    @Modifying
    @Query("DELETE FROM BoardScrap bs WHERE bs.board.id = :boardId AND bs.account.id = :accountId")
    int deleteByBoardIdAndAccountId(@Param("boardId") Long boardId, @Param("accountId") Long accountId);

    @Query("SELECT b FROM BoardScrap bs " +
            "JOIN bs.board b " +
            "JOIN FETCH b.account " +
            "LEFT JOIN FETCH b.section " +
            "WHERE bs.account.id = :accountId AND b.state = :state AND b.hiddenByReport = false " +
            "AND (:excludeBlocked = false OR b.account.id NOT IN :blockedAccountIds) " +
            "ORDER BY bs.regTime DESC")
    List<Board> findScrappedBoardsByAccountIdAndBoardState(@Param("accountId") Long accountId,
                                                         @Param("state") SoftDeleteState state,
                                                         @Param("excludeBlocked") boolean excludeBlocked,
                                                         @Param("blockedAccountIds") Collection<Long> blockedAccountIds);

    @Query(
            value = "SELECT b FROM BoardScrap bs " +
                    "JOIN bs.board b " +
                    "JOIN FETCH b.account " +
                    "LEFT JOIN FETCH b.section " +
                    "WHERE bs.account.id = :accountId AND b.state = :state AND b.hiddenByReport = false " +
                    "AND (:excludeBlocked = false OR b.account.id NOT IN :blockedAccountIds) " +
                    "ORDER BY bs.regTime DESC",
            countQuery = "SELECT COUNT(bs) FROM BoardScrap bs " +
                    "JOIN bs.board b " +
                    "WHERE bs.account.id = :accountId AND b.state = :state AND b.hiddenByReport = false " +
                    "AND (:excludeBlocked = false OR b.account.id NOT IN :blockedAccountIds)"
    )
    Page<Board> findScrappedBoardsPageByAccountIdAndBoardState(@Param("accountId") Long accountId,
                                                             @Param("state") SoftDeleteState state,
                                                             @Param("excludeBlocked") boolean excludeBlocked,
                                                             @Param("blockedAccountIds") Collection<Long> blockedAccountIds,
                                                             Pageable pageable);
}
