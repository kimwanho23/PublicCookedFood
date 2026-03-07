package kwh.PublicCookedFood.metrics.view;

import java.util.Collection;
import java.util.Map;

public interface ViewCounterService {

    long increaseBoardViewAndGet(Long boardId);

    long getBoardViewCount(Long boardId);

    Map<Long, Long> getBoardViewCounts(Collection<Long> boardIds);
}
