package kwh.PublicCookedFood.account.aop.action;

import kwh.PublicCookedFood.account.aop.AccountActionAuditType;
import kwh.PublicCookedFood.account.audit.BoardCommentAuditPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class BoardCommentAuditStrategy extends AbstractAccountActionAuditStrategy {

    public BoardCommentAuditStrategy(AccountActionAuditRecorder recorder) {
        super(createHandlers(recorder));
    }

    private static Map<AccountActionAuditType, Consumer<AccountActionAuditArgs>> createHandlers(
            AccountActionAuditRecorder recorder) {
        Map<AccountActionAuditType, Consumer<AccountActionAuditArgs>> handlers = newHandlers();

        handlers.put(AccountActionAuditType.BOARD_COMMENT_DELETE, args -> {
            Long accountId = args.asLong(0);
            Long boardId = args.asLong(1);
            Long commentId = args.asLong(2);
            log.info("action=board.comment_delete result=success accountId={} boardId={} commentId={}",
                    args.displayId(accountId), args.displayId(boardId), args.displayId(commentId));
            recorder.record(accountId, "BOARD_COMMENT_DELETE",
                    "boardId=" + args.displayId(boardId) + ",commentId=" + args.displayId(commentId));
        });
        handlers.put(AccountActionAuditType.BOARD_COMMENT_CREATE, args -> {
            BoardCommentAuditPayload payload = args.asType(1, BoardCommentAuditPayload.class);
            if (payload == null) {
                return;
            }
            Long accountId = args.asLong(0);
            log.info("action=board.comment_create result=success accountId={} boardId={} parentId={}",
                    args.displayId(accountId), payload.boardId(), payload.parentReference().auditValue());
            recorder.record(accountId, "BOARD_COMMENT_CREATE", payload.detailText());
        });
        handlers.put(AccountActionAuditType.BOARD_COMMENT_CREATE_FAILED, args -> {
            Long accountId = args.asLong(0);
            Long boardId = args.asLong(1);
            String reason = args.asString(2);
            log.warn("action=board.comment_create result=failed accountId={} boardId={} reason={}",
                    args.displayId(accountId), args.displayId(boardId), args.displayText(reason));
        });

        return handlers;
    }
}

