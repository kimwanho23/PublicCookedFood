package kwh.PublicCookedFood.common.error;

import org.springframework.http.HttpStatus;

public enum CommonErrorCode implements ErrorCode {
    VALIDATION_ERROR("COMMON_VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "요청 값이 유효하지 않습니다."),
    INVALID_REQUEST("COMMON_INVALID_REQUEST", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    AUTHENTICATION_REQUIRED("COMMON_AUTHENTICATION_REQUIRED", HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    ACCESS_DENIED("COMMON_ACCESS_DENIED", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    RESOURCE_NOT_FOUND("COMMON_RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    REQUEST_CONFLICT("COMMON_REQUEST_CONFLICT", HttpStatus.CONFLICT, "요청을 처리할 수 없습니다."),
    PAYLOAD_TOO_LARGE("COMMON_PAYLOAD_TOO_LARGE", HttpStatus.PAYLOAD_TOO_LARGE, "업로드 가능한 최대 파일 크기를 초과했습니다."),
    SERVICE_UNAVAILABLE("COMMON_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "현재 서비스를 사용할 수 없습니다."),
    STORAGE_ERROR("COMMON_STORAGE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 중 오류가 발생했습니다."),
    INTERNAL_SERVER_ERROR("COMMON_INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    CommonErrorCode(String code, HttpStatus status, String message) {
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
