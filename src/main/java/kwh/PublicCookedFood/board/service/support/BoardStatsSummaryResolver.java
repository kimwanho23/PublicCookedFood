package kwh.PublicCookedFood.board.service.support;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.repository.BoardStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class BoardStatsSummaryResolver {

    private final BoardStatsRepository boardStatsRepository;

    public Map<Long, BoardStatsSummary> resolve(Iterable<Board> boards) {
        if (boards == null) {
            return Map.of();
        }

        Map<Long, BoardStatsSummary> summaries = new LinkedHashMap<>();
        Set<Long> boardIds = new LinkedHashSet<>();
        for (Board board : boards) {
            if (board == null || board.getId() == null) {
                continue;
            }
            boardIds.add(board.getId());
            summaries.put(board.getId(), BoardStatsSummary.ZERO);
        }
        if (boardIds.isEmpty()) {
            return Map.of();
        }

        for (BoardStatsRepository.BoardStatsSummaryProjection projection : boardStatsRepository.findCounterSummariesByBoardIdIn(boardIds)) {
            if (projection.getBoardId() == null) {
                continue;
            }
            summaries.put(projection.getBoardId(), new BoardStatsSummary(
                    normalize(projection.getTotalViews()),
                    normalize(projection.getTotalLikes()),
                    normalize(projection.getTotalComments())
            ));
        }
        return Map.copyOf(summaries);
    }

    private long normalize(Long value) {
        return value == null ? 0L : Math.max(0L, value);
    }
}
