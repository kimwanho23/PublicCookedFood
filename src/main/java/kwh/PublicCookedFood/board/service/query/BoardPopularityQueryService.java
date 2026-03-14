package kwh.PublicCookedFood.board.service.query;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardPopularityQueryService {

    private final BoardRepository boardRepository;

    public List<Board> getPopularBoardsSince(LocalDateTime since, int limit) {
        int normalizedLimit = limit <= 0 ? 10 : Math.min(limit, 30);
        LocalDateTime baseline = since == null ? LocalDateTime.now().minusDays(7) : since;
        return boardRepository.findTopByStateAndRegTimeAfterOrderByPopularity(
                SoftDeleteState.ACTIVE,
                baseline,
                PageRequest.of(0, normalizedLimit)
        );
    }
}
