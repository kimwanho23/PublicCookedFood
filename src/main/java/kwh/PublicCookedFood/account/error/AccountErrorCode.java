package kwh.PublicCookedFood.account.error;

import kwh.PublicCookedFood.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AccountErrorCode implements ErrorCode {
    ACCOUNT_EMAIL_DUPLICATED("ACCOUNT_EMAIL_DUPLICATED", HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    ACCOUNT_NAME_DUPLICATED("ACCOUNT_NAME_DUPLICATED", HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    ACCOUNT_NOT_FOUND("ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND, "사용자 정보를 찾을 수 없습니다."),
    ACCOUNT_BLOCK_INVALID_REQUEST("ACCOUNT_BLOCK_INVALID_REQUEST", HttpStatus.BAD_REQUEST, "차단 대상 정보가 올바르지 않습니다."),
    ACCOUNT_BLOCK_SELF_FORBIDDEN("ACCOUNT_BLOCK_SELF_FORBIDDEN", HttpStatus.BAD_REQUEST, "본인은 차단할 수 없습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    AccountErrorCode(String code, HttpStatus status, String message) {
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
