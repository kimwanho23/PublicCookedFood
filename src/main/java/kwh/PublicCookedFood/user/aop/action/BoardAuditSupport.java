package kwh.PublicCookedFood.user.aop.action;

import org.slf4j.Logger;

final class BoardAuditSupport {

    private BoardAuditSupport() {
    }

    static void recordBoardAction(
            Logger logger,
            UserActionAuditArgs args,
            UserActionAuditRecorder recorder,
            String logMessage,
            String activityType) {
        Long userId = args.asLong(0);
        Long boardId = args.asLong(1);
        logger.info(logMessage, userId, boardId);
        recorder.record(userId, activityType, "boardId=" + args.safeId(boardId));
    }
}
