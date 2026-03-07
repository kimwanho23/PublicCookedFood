package kwh.PublicCookedFood.notification.error;

import kwh.PublicCookedFood.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum NotificationErrorCode implements ErrorCode {
    NOTIFICATION_DISABLED("NOTIFICATION_DISABLED", HttpStatus.CONFLICT, "알림 수신이 비활성화되어 있습니다."),
    NOTIFICATION_SSE_DISABLED("NOTIFICATION_SSE_DISABLED", HttpStatus.SERVICE_UNAVAILABLE, "알림 SSE가 비활성화되어 있습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    NotificationErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String message() {
        return message;
    }
}
