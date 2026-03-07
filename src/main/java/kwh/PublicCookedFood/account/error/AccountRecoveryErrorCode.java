package kwh.PublicCookedFood.account.error;

import kwh.PublicCookedFood.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AccountRecoveryErrorCode implements ErrorCode {
    PASSWORD_RESET_CODE_DELIVERY_FAILED("ACCOUNT_RECOVERY_PASSWORD_RESET_CODE_DELIVERY_FAILED",
            HttpStatus.SERVICE_UNAVAILABLE,
            "인증 코드 발송에 실패했습니다. 잠시 후 다시 시도해주세요."),
    PASSWORD_RESET_CODE_REQUIRED("ACCOUNT_RECOVERY_PASSWORD_RESET_CODE_REQUIRED",
            HttpStatus.CONFLICT,
            "인증 코드를 먼저 발급받아주세요."),
    PASSWORD_RESET_CODE_EXPIRED("ACCOUNT_RECOVERY_PASSWORD_RESET_CODE_EXPIRED",
            HttpStatus.CONFLICT,
            "인증 코드가 만료되었습니다. 다시 발급받아주세요.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    AccountRecoveryErrorCode(String code, HttpStatus status, String message) {
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
