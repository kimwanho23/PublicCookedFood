package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardReport;
import kwh.PublicCookedFood.board.domain.BoardReportStatus;
import kwh.PublicCookedFood.notification.domain.NotificationType;
import org.springframework.util.StringUtils;

public record ReportProcessedDispatchCommand(
        BoardReport report,
        NotificationType notificationType,
        String previewSeed) {

    private static final String DEFAULT_PREVIEW_SEED = "신고 처리 결과가 등록되었습니다.";

    public static ReportProcessedDispatchCommand from(BoardReport report) {
        return new ReportProcessedDispatchCommand(
                report,
                resolveNotificationType(report.getStatus()),
                resolvePreviewSeed(report)
        );
    }

    public ReportProcessedDispatchCommand {
        if (!notificationType.isReportResult()) {
            throw new IllegalStateException("신고 처리 결과 알림 타입이어야 합니다.");
        }
        NotificationType expectedType = switch (report.getStatus()) {
            case RESOLVED -> NotificationType.REPORT_RESOLVED;
            case REJECTED -> NotificationType.REPORT_REJECTED;
            case OPEN -> throw new IllegalStateException("신고 처리 결과 알림은 OPEN 상태를 허용하지 않습니다.");
        };
        if (notificationType != expectedType) {
            throw new IllegalStateException("신고 처리 결과와 알림 타입이 일치해야 합니다.");
        }
    }

    public Board board() {
        return report.getBoard();
    }

    public Account reporter() {
        return report.getReporter();
    }

    public Account processor() {
        return report.getProcessor();
    }

    private static NotificationType resolveNotificationType(BoardReportStatus status) {
        return switch (status) {
            case RESOLVED -> NotificationType.REPORT_RESOLVED;
            case REJECTED -> NotificationType.REPORT_REJECTED;
            case OPEN -> throw new IllegalStateException("신고 처리 알림 타입을 만들 수 없는 상태입니다: " + status);
        };
    }

    private static String resolvePreviewSeed(BoardReport report) {
        String processedNote = report.getProcessedNote();
        if (StringUtils.hasText(processedNote)) {
            return processedNote;
        }
        if (report.getReason() != null && StringUtils.hasText(report.getReason().getLabel())) {
            return report.getReason().getLabel();
        }
        return DEFAULT_PREVIEW_SEED;
    }
}
