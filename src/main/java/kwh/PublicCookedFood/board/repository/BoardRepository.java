package kwh.PublicCookedFood.board.repository;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long>, BoardRepositoryCustom {

    boolean existsByIdAndStateAndHiddenByReportFalse(Long id, SoftDeleteState state);

    @Query("SELECT b FROM Board b JOIN FETCH b.account LEFT JOIN FETCH b.section WHERE b.id = :id")
    Optional<Board> findByIdWithAccount(@Param("id") Long id);

    @Query("SELECT b FROM Board b JOIN FETCH b.account LEFT JOIN FETCH b.section " +
            "WHERE b.id = :id AND b.state = :state AND b.hiddenByReport = false")
    Optional<Board> findByIdWithAccountAndState(@Param("id") Long id, @Param("state") SoftDeleteState state);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Board b WHERE b.id = :id AND b.state = :state AND b.hiddenByReport = false")
    Optional<Board> findByIdAndStateAndHiddenByReportFalseForUpdate(@Param("id") Long id,
                                                                    @Param("state") SoftDeleteState state);

    @Query("SELECT b FROM Board b JOIN FETCH b.account LEFT JOIN FETCH b.section " +
            "WHERE b.id IN :ids AND b.state = :state AND b.hiddenByReport = false")
    List<Board> findAllByIdInWithAccountAndState(@Param("ids") Collection<Long> ids,
                                               @Param("state") SoftDeleteState state);

    boolean existsBySectionAndState(BoardSection section, SoftDeleteState state);

    long countByAccountIdAndState(Long accountId, SoftDeleteState state);

    long countByHiddenByReportTrue();

    @Modifying
    @Query("update Board p set p.state = :state where p.id = :id")
    void updateState(@Param("id") Long id, @Param("state") SoftDeleteState state);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Board b set b.section = :targetSection where b.section = :sourceSection")
    int reassignSection(@Param("sourceSection") BoardSection sourceSection,
                        @Param("targetSection") BoardSection targetSection);
}
