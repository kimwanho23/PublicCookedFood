package kwh.PublicCookedFood.notification.domain;

import lombok.Getter;

import java.util.Set;

@Getter
public enum NotificationType {
    BOARD_COMMENT(false),
    COMMENT_REPLY(false),
    BOARD_MENTION(false),
    COMMENT_MENTION(false),
    REPORT_RESOLVED(true),
    REPORT_REJECTED(true);

    private final boolean reportResult;

    NotificationType(boolean reportResult) {
        this.reportResult = reportResult;
    }

    public static Set<NotificationType> reportResultTypes() {
        return REPORT_RESULT_TYPES;
    }

    private static final Set<NotificationType> REPORT_RESULT_TYPES = Set.of(REPORT_RESOLVED, REPORT_REJECTED);
}
