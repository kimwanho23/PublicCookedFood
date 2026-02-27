package kwh.PublicCookedFood.user.aop.action;

import kwh.PublicCookedFood.user.aop.UserActionAuditType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class BoardLifecycleAuditStrategy extends AbstractUserActionAuditStrategy {

    public BoardLifecycleAuditStrategy(UserActionAuditRecorder recorder) {
        super(createHandlers(recorder));
    }

    private static Map<UserActionAuditType, Consumer<UserActionAuditArgs>> createHandlers(
            UserActionAuditRecorder recorder) {
        Map<UserActionAuditType, Consumer<UserActionAuditArgs>> handlers = newHandlers();

        handlers.put(UserActionAuditType.BOARD_CREATE, args -> BoardAuditSupport.recordBoardAction(
                log, args, recorder, "action=board.create result=success userId={} boardId={}", "BOARD_CREATE"));
        handlers.put(UserActionAuditType.BOARD_UPDATE, args -> BoardAuditSupport.recordBoardAction(
                log, args, recorder, "action=board.update result=success userId={} boardId={}", "BOARD_UPDATE"));
        handlers.put(UserActionAuditType.BOARD_DELETE, args -> BoardAuditSupport.recordBoardAction(
                log, args, recorder, "action=board.delete result=success userId={} boardId={}", "BOARD_DELETE"));

        return handlers;
    }
}
