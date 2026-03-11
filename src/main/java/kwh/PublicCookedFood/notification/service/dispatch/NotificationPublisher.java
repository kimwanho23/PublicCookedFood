package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.notification.domain.Notification;
import kwh.PublicCookedFood.notification.repository.NotificationRepository;
import kwh.PublicCookedFood.notification.service.NotificationSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final NotificationRepository notificationRepository;
    private final NotificationSseService notificationSseService;

    public Notification saveAndPublish(Notification notification) {
        Notification saved = notificationRepository.save(notification);
        publishAfterCommit(saved);
        return saved;
    }

    public void publishAll(Iterable<Notification> notifications) {
        for (Notification notification : notifications) {
            saveAndPublish(notification);
        }
    }

    private void publishAfterCommit(Notification notification) {
        notificationSseService.publishNotificationAfterCommit(
                notification.getReceiver().getId(),
                notification.getId()
        );
    }
}
