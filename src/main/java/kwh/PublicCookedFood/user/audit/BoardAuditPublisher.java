package kwh.PublicCookedFood.user.audit;

import kwh.PublicCookedFood.user.aop.UserActionAuditType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BoardAuditPublisher {

    private final AuditEventDispatcher dispatcher;

    public void boardCreate(Long userId, Long boardId) {
        dispatcher.publishUserAction(UserActionAuditType.BOARD_CREATE, userId, boardId);
    }

    public void boardUpdate(Long userId, Long boardId) {
        dispatcher.publishUserAction(UserActionAuditType.BOARD_UPDATE, userId, boardId);
    }

    public void boardDelete(Long userId, Long boardId) {
        dispatcher.publishUserAction(UserActionAuditType.BOARD_DELETE, userId, boardId);
    }

    public void boardCommentDelete(Long userId, Long boardId, Long commentId) {
        dispatcher.publishUserAction(UserActionAuditType.BOARD_COMMENT_DELETE, userId, boardId, commentId);
    }

    public void boardCommentCreate(Long userId, Long boardId, Long commentId, Long parentId) {
        dispatcher.publishUserAction(UserActionAuditType.BOARD_COMMENT_CREATE, userId, boardId, commentId, parentId);
    }

    public void boardCommentCreateFailed(Long userId, Long boardId, String reason) {
        dispatcher.publishUserAction(UserActionAuditType.BOARD_COMMENT_CREATE_FAILED, userId, boardId, reason);
    }

    public void boardScrapAdd(Long userId, Long boardId) {
        dispatcher.publishUserAction(UserActionAuditType.BOARD_SCRAP_ADD, userId, boardId);
    }

    public void boardScrapRemove(Long userId, Long boardId) {
        dispatcher.publishUserAction(UserActionAuditType.BOARD_SCRAP_REMOVE, userId, boardId);
    }

    public void boardReportCreate(Long userId, Long boardId, String reason) {
        dispatcher.publishUserAction(UserActionAuditType.BOARD_REPORT_CREATE, userId, boardId, reason);
    }

    public void boardReportCreateFailed(Long userId, Long boardId, String reason) {
        dispatcher.publishUserAction(UserActionAuditType.BOARD_REPORT_CREATE_FAILED, userId, boardId, reason);
    }

    public void boardLikeToggle(Long userId, Long boardId) {
        dispatcher.publishUserAction(UserActionAuditType.BOARD_LIKE_TOGGLE, userId, boardId);
    }

    public void boardPolicyUpdate(Long userId, Integer featuredLikeThreshold, String thumbnailDisplayMode) {
        dispatcher.publishUserAction(
                UserActionAuditType.BOARD_POLICY_UPDATE,
                userId,
                featuredLikeThreshold,
                thumbnailDisplayMode
        );
    }

    public void boardReportStatusUpdate(Long userId, Long reportId, String status) {
        dispatcher.publishUserAction(UserActionAuditType.BOARD_REPORT_STATUS_UPDATE, userId, reportId, status);
    }
}
