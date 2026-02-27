package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardReport;
import kwh.PublicCookedFood.board.domain.Comments;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class NotificationDispatchFacade {

    private final Map<NotificationDispatchType, NotificationDispatchStrategy> strategyMap =
            new EnumMap<>(NotificationDispatchType.class);

    public NotificationDispatchFacade(List<NotificationDispatchStrategy> strategies) {
        for (NotificationDispatchStrategy strategy : strategies) {
            NotificationDispatchType type = strategy.type();
            NotificationDispatchStrategy existing = strategyMap.putIfAbsent(type, strategy);
            if (existing != null) {
                throw new IllegalStateException("Duplicate notification dispatch strategy for type: " + type);
            }
        }
    }

    public void dispatchOnNewComment(Comments comment) {
        dispatch(NotificationDispatchType.NEW_COMMENT, NotificationDispatchContext.forNewComment(comment));
    }

    public void dispatchOnBoardCreated(Board board) {
        dispatch(NotificationDispatchType.BOARD_CREATED, NotificationDispatchContext.forBoardCreated(board));
    }

    public void dispatchOnReportProcessed(BoardReport report) {
        dispatch(NotificationDispatchType.REPORT_PROCESSED, NotificationDispatchContext.forReportProcessed(report));
    }

    private void dispatch(NotificationDispatchType type, NotificationDispatchContext context) {
        NotificationDispatchStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new IllegalStateException("Missing notification dispatch strategy for type: " + type);
        }
        strategy.dispatch(context);
    }
}

