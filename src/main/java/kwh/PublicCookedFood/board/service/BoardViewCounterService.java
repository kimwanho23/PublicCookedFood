package kwh.PublicCookedFood.board.service;

import java.util.Collection;
import java.util.Map;

public interface BoardViewCounterService {

    long increaseBoardViewAndGet(Long boardId);

    long getBoardViewCount(Long boardId);

    Map<Long, Long> getBoardViewCounts(Collection<Long> boardIds);
}
