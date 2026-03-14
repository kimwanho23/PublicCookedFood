package kwh.PublicCookedFood.board.repository;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface BoardRepositoryCustom {

    Page<Board> findPageWithAccount(BoardPageQuery query);

    List<Long> findTopBoardIdsForSnapshot(BoardSnapshotQuery query);

    List<Board> findTopByStateAndRegTimeAfterOrderByPopularity(SoftDeleteState state,
                                                               LocalDateTime since,
                                                               Pageable pageable);
}
