package kwh.PublicCookedFood.notification.service;

import org.springframework.transaction.annotation.Transactional;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardReport;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.notification.dto.response.NotificationResponse;
import kwh.PublicCookedFood.notification.repository.NotificationRepository;
import kwh.PublicCookedFood.notification.service.dispatch.NotificationDispatchFacade;
import kwh.PublicCookedFood.user.audit.NotificationAuditPublisher;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final NotificationAuditPublisher notificationAuditPublisher;
    private final NotificationDispatchFacade notificationDispatchFacade;
    private final NotificationSseService notificationSseService;

    public SseEmitter subscribe(Long receiverId) {
        if (!notificationSseService.isSseEnabled()) {
            throw new IllegalStateException("알림 SSE가 비활성화되어 있습니다.");
        }
        if (!isNotificationEnabled(receiverId)) {
            throw new IllegalStateException("알림 수신이 비활성화되어 있습니다.");
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
        if (unreadOnly) {
            return notificationRepository.findByReceiverIdAndIsReadFalseOrderByRegTimeDesc(receiverId, pageable)
                    .map(NotificationResponse::from);
        }
        return notificationRepository.findByReceiverIdOrderByRegTimeDesc(receiverId, pageable)
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long receiverId) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(receiverId);
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
    public boolean updateNotificationEnabled(Long userId, boolean enabled) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자 정보를 찾을 수 없습니다."));
        user.updateNotificationEnabled(enabled);
        notificationAuditPublisher.notificationSettingUpdate(userId, enabled);
        if (!enabled) {
            notificationSseService.clearEmitters(userId);
        }
        return user.isNotificationEnabled();
    }

    @Transactional(readOnly = true)
    public boolean isNotificationEnabled(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자 정보를 찾을 수 없습니다."));
        return user.isNotificationEnabled();
    }
}
