package kwh.PublicCookedFood.user.aop.action;

import kwh.PublicCookedFood.user.aop.UserActionAuditType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class BoardInteractionAuditStrategy extends AbstractUserActionAuditStrategy {

    public BoardInteractionAuditStrategy(UserActionAuditRecorder recorder) {
        super(createHandlers(recorder));
    }

    private static Map<UserActionAuditType, Consumer<UserActionAuditArgs>> createHandlers(
            UserActionAuditRecorder recorder) {
        Map<UserActionAuditType, Consumer<UserActionAuditArgs>> handlers = newHandlers();

        handlers.put(UserActionAuditType.BOARD_SCRAP_ADD, args -> BoardAuditSupport.recordBoardAction(
                log, args, recorder, "action=board.scrap_add result=success userId={} boardId={}", "BOARD_SCRAP_ADD"));
        handlers.put(UserActionAuditType.BOARD_SCRAP_REMOVE, args -> BoardAuditSupport.recordBoardAction(
                log, args, recorder, "action=board.scrap_remove result=success userId={} boardId={}", "BOARD_SCRAP_REMOVE"));
        handlers.put(UserActionAuditType.BOARD_LIKE_TOGGLE, args -> BoardAuditSupport.recordBoardAction(
                log, args, recorder, "action=board.like_toggle result=success userId={} boardId={}", "BOARD_LIKE_TOGGLE"));

        return handlers;
    }
}
