package kwh.PublicCookedFood.notification.service;

import org.springframework.transaction.annotation.Transactional;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardReport;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.account.error.AccountErrorCode;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.notification.dto.response.NotificationResponse;
import kwh.PublicCookedFood.notification.error.NotificationErrorCode;
import kwh.PublicCookedFood.notification.repository.NotificationRepository;
import kwh.PublicCookedFood.notification.service.dispatch.BoardCreatedNotificationDispatchStrategy;
import kwh.PublicCookedFood.notification.service.dispatch.NewCommentDispatchCommand;
import kwh.PublicCookedFood.notification.service.dispatch.NewCommentNotificationDispatchStrategy;
import kwh.PublicCookedFood.notification.service.dispatch.ReportProcessedDispatchCommand;
import kwh.PublicCookedFood.notification.service.dispatch.ReportProcessedNotificationDispatchStrategy;
import kwh.PublicCookedFood.account.audit.NotificationAuditPublisher;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AccountRepository accountRepository;
    private final NotificationAuditPublisher notificationAuditPublisher;
    private final NewCommentNotificationDispatchStrategy newCommentStrategy;
    private final BoardCreatedNotificationDispatchStrategy boardCreatedStrategy;
    private final ReportProcessedNotificationDispatchStrategy reportProcessedStrategy;
    private final NotificationSseService notificationSseService;
    private final NotificationViewSupport notificationViewSupport;

    public SseEmitter subscribe(Long receiverId) {
        if (!notificationSseService.isSseEnabled()) {
            throw new AppException(NotificationErrorCode.NOTIFICATION_SSE_DISABLED);
        }
        return notificationSseService.subscribe(receiverId);
    }

    @Transactional
    public void notifyOnNewComment(Comments comment) {
        newCommentStrategy.dispatch(NewCommentDispatchCommand.from(comment));
    }

    @Transactional
    public void notifyOnBoardCreated(Board board) {
        boardCreatedStrategy.dispatch(board);
    }

    @Transactional
    public void notifyOnReportProcessed(BoardReport report) {
        reportProcessedStrategy.dispatch(ReportProcessedDispatchCommand.from(report));
    }

    @Transactional
    public void markAsRead(Long receiverId, Long notificationId) {
        int updatedCount = notificationRepository.markAsRead(notificationId, receiverId, LocalDateTime.now());
        if (updatedCount > 0) {
            return;
        }
        if (notificationRepository.existsByIdAndReceiverId(notificationId, receiverId)) {
            return;
        }
        throw new AppException(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Transactional
    public int markAllAsRead(Long receiverId) {
        return notificationRepository.markAllAsRead(receiverId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(long receiverId, Pageable pageable, boolean unreadOnly) {
        return notificationViewSupport.loadVisibleNotificationPage(receiverId, pageable, unreadOnly);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(long receiverId) {
        return notificationViewSupport.countVisibleUnreadNotifications(receiverId);
    }

    @Transactional
    public void deleteNotification(Long receiverId, Long notificationId) {
        long deletedCount = notificationRepository.deleteByIdAndReceiverId(notificationId, receiverId);
        if (deletedCount <= 0) {
            throw new AppException(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
        }
    }

    @Transactional
    public long deleteAllNotifications(Long receiverId, boolean unreadOnly) {
        if (unreadOnly) {
            return notificationRepository.deleteByReceiverIdAndIsReadFalse(receiverId);
        }
        return notificationRepository.deleteByReceiverId(receiverId);
    }

    @Transactional
    public boolean updateNotificationEnabled(Long accountId, boolean enabled) {
        Account account = getAccount(accountId);
        account.updateNotificationEnabled(enabled);
        notificationAuditPublisher.notificationSettingUpdate(accountId, enabled);
        if (!enabled) {
            notificationSseService.clearEmittersAfterCommit(accountId);
        }
        return account.isNotificationEnabled();
    }

    @Transactional(readOnly = true)
    public boolean isNotificationEnabled(Long accountId) {
        return getAccount(accountId).isNotificationEnabled();
    }

    private Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(AccountErrorCode.ACCOUNT_NOT_FOUND));
    }
}
