package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.board.domain.BoardReport;
import kwh.PublicCookedFood.notification.domain.Notification;
import kwh.PublicCookedFood.notification.domain.NotificationType;
import org.springframework.stereotype.Component;

@Component
public class ReportProcessedNotificationDispatchStrategy implements NotificationDispatchStrategy {

    private final NotificationDispatchSupport support;

    public ReportProcessedNotificationDispatchStrategy(NotificationDispatchSupport support) {
        this.support = support;
    }

    @Override
    public NotificationDispatchType type() {
        return NotificationDispatchType.REPORT_PROCESSED;
    }

    @Override
    public void dispatch(NotificationDispatchContext context) {
        BoardReport report = context.report();
        if (report == null
                || report.getReporter() == null
                || report.getProcessor() == null
                || report.getBoard() == null
                || report.getReporter().getId() == null
                || report.getProcessor().getId() == null) {
            return;
        }
        if (report.getReporter().getId().equals(report.getProcessor().getId())) {
            return;
        }

        NotificationType notificationType = support.resolveReportNotificationType(report.getStatus());
        if (notificationType == null) {
            return;
        }
        if (!support.canReceiveNotification(report.getReporter(), report.getProcessor())) {
            return;
        }

        String previewSeed = report.getProcessedNote();
        if (previewSeed == null || previewSeed.isBlank()) {
            previewSeed = report.getReason() == null ? "신고 처리 결과가 등록되었습니다." : report.getReason().getLabel();
        }
        String preview = support.buildPreview(previewSeed);

        support.saveAndPublish(Notification.reportResult(
                report.getReporter(),
                report.getProcessor(),
                report.getBoard(),
                notificationType,
                preview
        ));
    }
}

