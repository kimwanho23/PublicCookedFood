package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.notification.domain.Notification;
import org.springframework.stereotype.Component;

@Component
public class ReportProcessedNotificationDispatchStrategy {

    private final NotificationPublisher notificationPublisher;
    private final NotificationPreviewFactory previewFactory;
    private final NotificationReceiverPolicy receiverPolicy;

    public ReportProcessedNotificationDispatchStrategy(NotificationPublisher notificationPublisher,
                                                       NotificationPreviewFactory previewFactory,
                                                       NotificationReceiverPolicy receiverPolicy) {
        this.notificationPublisher = notificationPublisher;
        this.previewFactory = previewFactory;
        this.receiverPolicy = receiverPolicy;
    }

    public void dispatch(ReportProcessedDispatchCommand command) {
        Account reporter = command.reporter();
        Account processor = command.processor();
        if (receiverPolicy.isSameAccount(reporter, processor)) {
            return;
        }

        if (!receiverPolicy.canReceiveFromActor(reporter, processor)) {
            return;
        }

        notificationPublisher.saveAndPublish(createNotification(command));
    }

    private Notification createNotification(ReportProcessedDispatchCommand command) {
        return Notification.reportResult(
                command.reporter(),
                command.processor(),
                command.board(),
                command.notificationType(),
                previewFactory.buildPreview(command.previewSeed())
        );
    }
}
