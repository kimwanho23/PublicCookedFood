package kwh.PublicCookedFood.user.aop.action;

import kwh.PublicCookedFood.user.aop.UserActionAuditType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Slf4j
public class BoardCommentAuditStrategy extends AbstractUserActionAuditStrategy {

    public BoardCommentAuditStrategy(UserActionAuditRecorder recorder) {
        super(createHandlers(recorder));
    }

    private static Map<UserActionAuditType, Consumer<UserActionAuditArgs>> createHandlers(
            UserActionAuditRecorder recorder) {
        Map<UserActionAuditType, Consumer<UserActionAuditArgs>> handlers = newHandlers();

        handlers.put(UserActionAuditType.BOARD_COMMENT_DELETE, args -> {
            Long userId = args.asLong(0);
            Long boardId = args.asLong(1);
            Long commentId = args.asLong(2);
            log.info("action=board.comment_delete result=success userId={} boardId={} commentId={}",
                    userId, boardId, commentId);
            recorder.record(userId, "BOARD_COMMENT_DELETE",
                    "boardId=" + args.safeId(boardId) + ",commentId=" + args.safeId(commentId));
        });
        handlers.put(UserActionAuditType.BOARD_COMMENT_CREATE, args -> {
            Long userId = args.asLong(0);
            Long boardId = args.asLong(1);
            Long commentId = args.asLong(2);
            Long parentId = args.asLong(3);
            log.info("action=board.comment_create result=success userId={} boardId={} parentId={}",
                    userId, boardId, parentId);
            recorder.record(userId, "BOARD_COMMENT_CREATE",
                    "boardId=" + args.safeId(boardId)
                            + ",commentId=" + args.safeId(commentId)
                            + ",parentId=" + args.safeId(parentId));
        });
        handlers.put(UserActionAuditType.BOARD_COMMENT_CREATE_FAILED, args -> {
            Long userId = args.asLong(0);
            Long boardId = args.asLong(1);
            String reason = args.asString(2);
            log.warn("action=board.comment_create result=failed userId={} boardId={} reason={}",
                    userId, boardId, reason);
        });

        return handlers;
    }
}
