package kwh.PublicCookedFood.account.audit;

import kwh.PublicCookedFood.account.aop.AccountActionAuditType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BoardAuditPublisher {

    private final AuditEventDispatcher dispatcher;

    public void boardCreate(Long accountId, Long boardId) {
        dispatcher.publishAccountAction(AccountActionAuditType.BOARD_CREATE, accountId, boardId);
    }

    public void boardUpdate(Long accountId, Long boardId) {
        dispatcher.publishAccountAction(AccountActionAuditType.BOARD_UPDATE, accountId, boardId);
    }

    public void boardDelete(Long accountId, Long boardId) {
        dispatcher.publishAccountAction(AccountActionAuditType.BOARD_DELETE, accountId, boardId);
    }

    public void boardCommentDelete(Long accountId, Long boardId, Long commentId) {
        dispatcher.publishAccountAction(AccountActionAuditType.BOARD_COMMENT_DELETE, accountId, boardId, commentId);
    }

    public void boardCommentCreate(Long accountId, Long boardId, Long commentId, Long parentId) {
        dispatcher.publishAccountAction(AccountActionAuditType.BOARD_COMMENT_CREATE, accountId, boardId, commentId, parentId);
    }

    public void boardCommentCreateFailed(Long accountId, Long boardId, String reason) {
        dispatcher.publishAccountAction(AccountActionAuditType.BOARD_COMMENT_CREATE_FAILED, accountId, boardId, reason);
    }

    public void boardScrapAdd(Long accountId, Long boardId) {
        dispatcher.publishAccountAction(AccountActionAuditType.BOARD_SCRAP_ADD, accountId, boardId);
    }

    public void boardScrapRemove(Long accountId, Long boardId) {
        dispatcher.publishAccountAction(AccountActionAuditType.BOARD_SCRAP_REMOVE, accountId, boardId);
    }

    public void boardReportCreate(Long accountId, Long boardId, String reason) {
        dispatcher.publishAccountAction(AccountActionAuditType.BOARD_REPORT_CREATE, accountId, boardId, reason);
    }

    public void boardReportCreateFailed(Long accountId, Long boardId, String reason) {
        dispatcher.publishAccountAction(AccountActionAuditType.BOARD_REPORT_CREATE_FAILED, accountId, boardId, reason);
    }

    public void boardLikeToggle(Long accountId, Long boardId) {
        dispatcher.publishAccountAction(AccountActionAuditType.BOARD_LIKE_TOGGLE, accountId, boardId);
    }

    public void boardPolicyUpdate(Long accountId, Integer featuredLikeThreshold, String thumbnailDisplayMode) {
        dispatcher.publishAccountAction(
                AccountActionAuditType.BOARD_POLICY_UPDATE,
                accountId,
                featuredLikeThreshold,
                thumbnailDisplayMode
        );
    }

    public void boardReportStatusUpdate(Long accountId, Long reportId, String status) {
        dispatcher.publishAccountAction(AccountActionAuditType.BOARD_REPORT_STATUS_UPDATE, accountId, reportId, status);
    }
}

