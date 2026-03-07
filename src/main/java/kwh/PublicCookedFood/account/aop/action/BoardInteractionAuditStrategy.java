package kwh.PublicCookedFood.account.aop.action;

import kwh.PublicCookedFood.account.aop.AccountActionAuditType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class BoardInteractionAuditStrategy extends AbstractAccountActionAuditStrategy {

    public BoardInteractionAuditStrategy(AccountActionAuditRecorder recorder) {
        super(createHandlers(recorder));
    }

    private static Map<AccountActionAuditType, Consumer<AccountActionAuditArgs>> createHandlers(
            AccountActionAuditRecorder recorder) {
        Map<AccountActionAuditType, Consumer<AccountActionAuditArgs>> handlers = newHandlers();

        handlers.put(AccountActionAuditType.BOARD_SCRAP_ADD, args -> BoardAuditSupport.recordBoardAction(
                log, args, recorder, "action=board.scrap_add result=success accountId={} boardId={}", "BOARD_SCRAP_ADD"));
        handlers.put(AccountActionAuditType.BOARD_SCRAP_REMOVE, args -> BoardAuditSupport.recordBoardAction(
                log, args, recorder, "action=board.scrap_remove result=success accountId={} boardId={}", "BOARD_SCRAP_REMOVE"));
        handlers.put(AccountActionAuditType.BOARD_LIKE_TOGGLE, args -> BoardAuditSupport.recordBoardAction(
                log, args, recorder, "action=board.like_toggle result=success accountId={} boardId={}", "BOARD_LIKE_TOGGLE"));

        return handlers;
    }
}

