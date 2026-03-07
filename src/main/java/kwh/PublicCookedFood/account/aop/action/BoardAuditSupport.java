package kwh.PublicCookedFood.account.aop.action;

import org.slf4j.Logger;

final class BoardAuditSupport {

    private BoardAuditSupport() {
    }

    static void recordBoardAction(
            Logger logger,
            AccountActionAuditArgs args,
            AccountActionAuditRecorder recorder,
            String logMessage,
            String activityType) {
        Long accountId = args.asLong(0);
        Long boardId = args.asLong(1);
        logger.info(logMessage, accountId, boardId);
        recorder.record(accountId, activityType, "boardId=" + args.safeId(boardId));
    }
}

