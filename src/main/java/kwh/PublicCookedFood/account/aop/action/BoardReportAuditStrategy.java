package kwh.PublicCookedFood.account.aop.action;

import kwh.PublicCookedFood.account.aop.AccountActionAuditType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class BoardReportAuditStrategy extends AbstractAccountActionAuditStrategy {

    public BoardReportAuditStrategy(AccountActionAuditRecorder recorder) {
        super(createHandlers(recorder));
    }

    private static Map<AccountActionAuditType, Consumer<AccountActionAuditArgs>> createHandlers(
            AccountActionAuditRecorder recorder) {
        Map<AccountActionAuditType, Consumer<AccountActionAuditArgs>> handlers = newHandlers();

        handlers.put(AccountActionAuditType.BOARD_REPORT_CREATE, args -> {
            Long accountId = args.asLong(0);
            Long boardId = args.asLong(1);
            String reason = args.asString(2);
            log.info("action=board.report result=success accountId={} boardId={} reportReason={}",
                    args.displayId(accountId), args.displayId(boardId), args.displayText(reason));
            recorder.record(accountId, "BOARD_REPORT_CREATE",
                    "boardId=" + args.displayId(boardId) + ",reason=" + args.displayText(reason));
        });
        handlers.put(AccountActionAuditType.BOARD_REPORT_CREATE_FAILED, args -> {
            Long accountId = args.asLong(0);
            Long boardId = args.asLong(1);
            String reason = args.asString(2);
            log.warn("action=board.report result=failed accountId={} boardId={} reason={}",
                    args.displayId(accountId), args.displayId(boardId), args.displayText(reason));
        });
        handlers.put(AccountActionAuditType.BOARD_REPORT_STATUS_UPDATE, args -> {
            Long accountId = args.asLong(0);
            Long reportId = args.asLong(1);
            String status = args.asString(2);
            log.info("action=board.report_status_update result=success accountId={} reportId={} status={}",
                    args.displayId(accountId), args.displayId(reportId), args.displayText(status));
            recorder.record(accountId, "BOARD_REPORT_STATUS_UPDATE",
                    "reportId=" + args.displayId(reportId) + ",status=" + args.displayText(status));
        });

        return handlers;
    }
}

