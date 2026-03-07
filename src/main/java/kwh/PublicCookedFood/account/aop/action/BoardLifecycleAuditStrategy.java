package kwh.PublicCookedFood.account.aop.action;

import kwh.PublicCookedFood.account.aop.AccountActionAuditType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class BoardLifecycleAuditStrategy extends AbstractAccountActionAuditStrategy {

    public BoardLifecycleAuditStrategy(AccountActionAuditRecorder recorder) {
        super(createHandlers(recorder));
    }

    private static Map<AccountActionAuditType, Consumer<AccountActionAuditArgs>> createHandlers(
            AccountActionAuditRecorder recorder) {
        Map<AccountActionAuditType, Consumer<AccountActionAuditArgs>> handlers = newHandlers();

        handlers.put(AccountActionAuditType.BOARD_CREATE, args -> BoardAuditSupport.recordBoardAction(
                log, args, recorder, "action=board.create result=success accountId={} boardId={}", "BOARD_CREATE"));
        handlers.put(AccountActionAuditType.BOARD_UPDATE, args -> BoardAuditSupport.recordBoardAction(
                log, args, recorder, "action=board.update result=success accountId={} boardId={}", "BOARD_UPDATE"));
        handlers.put(AccountActionAuditType.BOARD_DELETE, args -> BoardAuditSupport.recordBoardAction(
                log, args, recorder, "action=board.delete result=success accountId={} boardId={}", "BOARD_DELETE"));

        return handlers;
    }
}

