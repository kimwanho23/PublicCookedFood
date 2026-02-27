package kwh.PublicCookedFood.user.aop.action;

import kwh.PublicCookedFood.user.aop.UserActionAuditType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class BoardReportAuditStrategy extends AbstractUserActionAuditStrategy {

    public BoardReportAuditStrategy(UserActionAuditRecorder recorder) {
        super(createHandlers(recorder));
    }

    private static Map<UserActionAuditType, Consumer<UserActionAuditArgs>> createHandlers(
            UserActionAuditRecorder recorder) {
        Map<UserActionAuditType, Consumer<UserActionAuditArgs>> handlers = newHandlers();

        handlers.put(UserActionAuditType.BOARD_REPORT_CREATE, args -> {
            Long userId = args.asLong(0);
            Long boardId = args.asLong(1);
            String reason = args.asString(2);
            log.info("action=board.report result=success userId={} boardId={} reportReason={}", userId, boardId, reason);
            recorder.record(userId, "BOARD_REPORT_CREATE",
                    "boardId=" + args.safeId(boardId) + ",reason=" + args.safeText(reason));
        });
        handlers.put(UserActionAuditType.BOARD_REPORT_CREATE_FAILED, args -> {
            Long userId = args.asLong(0);
            Long boardId = args.asLong(1);
            String reason = args.asString(2);
            log.warn("action=board.report result=failed userId={} boardId={} reason={}", userId, boardId, reason);
        });
        handlers.put(UserActionAuditType.BOARD_REPORT_STATUS_UPDATE, args -> {
            Long userId = args.asLong(0);
            Long reportId = args.asLong(1);
            String status = args.asString(2);
            log.info("action=board.report_status_update result=success userId={} reportId={} status={}",
                    userId, reportId, status);
            recorder.record(userId, "BOARD_REPORT_STATUS_UPDATE",
                    "reportId=" + args.safeId(reportId) + ",status=" + args.safeText(status));
        });

        return handlers;
    }
}
