package kwh.PublicCookedFood.notification.service;

import org.springframework.transaction.annotation.Transactional;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardReport;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.notification.dto.response.NotificationResponse;
import kwh.PublicCookedFood.notification.error.NotificationErrorCode;
import kwh.PublicCookedFood.notification.repository.NotificationRepository;
import kwh.PublicCookedFood.notification.service.dispatch.NotificationDispatchFacade;
import kwh.PublicCookedFood.account.audit.NotificationAuditPublisher;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AccountRepository accountRepository;
    private final NotificationAuditPublisher notificationAuditPublisher;
    private final NotificationDispatchFacade notificationDispatchFacade;
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
        notificationDispatchFacade.dispatchOnNewComment(comment);
    }

    @Transactional
    public void notifyOnBoardCreated(Board board) {
        notificationDispatchFacade.dispatchOnBoardCreated(board);
    }

    @Transactional
    public void notifyOnReportProcessed(BoardReport report) {
        notificationDispatchFacade.dispatchOnReportProcessed(report);
    }

    @Transactional
    public void markAsRead(Long receiverId, Long notificationId) {
        notificationRepository.markAsRead(notificationId, receiverId, LocalDateTime.now());
    }

    @Transactional
    public int markAllAsRead(Long receiverId) {
        return notificationRepository.markAllAsRead(receiverId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long receiverId, Pageable pageable, boolean unreadOnly) {
        return notificationViewSupport.loadVisibleNotificationPage(receiverId, pageable, unreadOnly);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long receiverId) {
        return notificationViewSupport.countVisibleUnreadNotifications(receiverId);
    }

    @Transactional
    public void deleteNotification(Long receiverId, Long notificationId) {
        notificationRepository.deleteByIdAndReceiverId(notificationId, receiverId);
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
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NoSuchElementException("사용자 정보를 찾을 수 없습니다."));
        account.updateNotificationEnabled(enabled);
        notificationAuditPublisher.notificationSettingUpdate(accountId, enabled);
        if (!enabled) {
            notificationSseService.clearEmitters(accountId);
        }
        return account.isNotificationEnabled();
    }

    @Transactional(readOnly = true)
    public boolean isNotificationEnabled(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NoSuchElementException("사용자 정보를 찾을 수 없습니다."));
        return account.isNotificationEnabled();
    }
}
