package kwh.PublicCookedFood.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.CommonErrorCode;
import kwh.PublicCookedFood.common.error.ErrorCode;
import kwh.PublicCookedFood.storage.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(annotations = RestController.class)
public class GlobalApiExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiErrorResponse> handleAppException(AppException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        if (errorCode.status().is5xxServerError()) {
            log.error("Application exception. uri={}, code={}", request.getRequestURI(), errorCode.code(), e);
        }
        return buildResponse(errorCode, defaultMessage(e.getMessage(), errorCode.message()), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                         HttpServletRequest request) {
        return buildResponse(CommonErrorCode.VALIDATION_ERROR, buildValidationMessage(e.getBindingResult()), request);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBindException(BindException e, HttpServletRequest request) {
        return buildResponse(CommonErrorCode.VALIDATION_ERROR, buildValidationMessage(e.getBindingResult()), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException e,
                                                                      HttpServletRequest request) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        return buildResponse(CommonErrorCode.VALIDATION_ERROR,
                message.isBlank() ? CommonErrorCode.VALIDATION_ERROR.message() : message,
                request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                                               HttpServletRequest request) {
        return buildResponse(CommonErrorCode.INVALID_REQUEST, "요청 값의 타입이 올바르지 않습니다.", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException e,
                                                                  HttpServletRequest request) {
        return buildResponse(CommonErrorCode.INVALID_REQUEST,
                defaultMessage(e.getMessage(), CommonErrorCode.INVALID_REQUEST.message()),
                request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException e,
                                                                HttpServletRequest request) {
        return buildResponse(CommonErrorCode.PAYLOAD_TOO_LARGE,
                CommonErrorCode.PAYLOAD_TOO_LARGE.message(),
                request);
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiErrorResponse> handleStorageException(StorageException e, HttpServletRequest request) {
        log.error("Storage exception. uri={}", request.getRequestURI(), e);
        return buildResponse(CommonErrorCode.STORAGE_ERROR,
                defaultMessage(e.getMessage(), CommonErrorCode.STORAGE_ERROR.message()),
                request);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiErrorResponse> handleNoSuchElement(NoSuchElementException e, HttpServletRequest request) {
        return buildResponse(CommonErrorCode.RESOURCE_NOT_FOUND,
                CommonErrorCode.RESOURCE_NOT_FOUND.message(),
                request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException e,
                                                                          HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }
        ErrorCode errorCode = resolveErrorCode(status);
        return buildResponse(status,
                errorCode.code(),
                defaultMessage(e.getReason(), errorCode.message()),
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpected(Exception e, HttpServletRequest request) {
        if (isClientAbortException(e)) {
            log.debug("Client disconnected during response write. uri={}", request.getRequestURI());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        log.error("Unhandled exception. uri={}", request.getRequestURI(), e);
        return buildResponse(CommonErrorCode.INTERNAL_SERVER_ERROR,
                CommonErrorCode.INTERNAL_SERVER_ERROR.message(),
                request);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(ErrorCode errorCode,
                                                           String message,
                                                           HttpServletRequest request) {
        return buildResponse(errorCode.status(), errorCode.code(), message, request);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status,
                                                           String code,
                                                           String message,
                                                           HttpServletRequest request) {
        return ResponseEntity.status(status).body(
                new ApiErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        code,
                        message,
                        request.getRequestURI()
                )
        );
    }

    private ErrorCode resolveErrorCode(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> CommonErrorCode.INVALID_REQUEST;
            case UNAUTHORIZED -> CommonErrorCode.AUTHENTICATION_REQUIRED;
            case FORBIDDEN -> CommonErrorCode.ACCESS_DENIED;
            case NOT_FOUND -> CommonErrorCode.RESOURCE_NOT_FOUND;
            case CONFLICT -> CommonErrorCode.REQUEST_CONFLICT;
            case PAYLOAD_TOO_LARGE -> CommonErrorCode.PAYLOAD_TOO_LARGE;
            case SERVICE_UNAVAILABLE -> CommonErrorCode.SERVICE_UNAVAILABLE;
            default -> status.is4xxClientError()
                    ? CommonErrorCode.INVALID_REQUEST
                    : CommonErrorCode.INTERNAL_SERVER_ERROR;
        };
    }

    private String buildValidationMessage(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(this::fieldErrorMessage)
                .collect(Collectors.collectingAndThen(Collectors.joining(", "),
                        joined -> joined.isBlank() ? "요청 값이 유효하지 않습니다." : joined));
    }

    private String fieldErrorMessage(FieldError error) {
        String defaultMessage = error.getDefaultMessage();
        if (defaultMessage == null || defaultMessage.isBlank()) {
            return error.getField() + " 값이 유효하지 않습니다.";
        }
        return defaultMessage;
    }

    private String defaultMessage(String candidate, String fallback) {
        if (candidate == null || candidate.isBlank()) {
            return fallback;
        }
        return candidate;
    }

    private boolean isClientAbortException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String className = current.getClass().getName();
            if (className.contains("ClientAbortException") || className.contains("AsyncRequestNotUsableException")) {
                return true;
            }

            String message = current.getMessage();
            if (message != null && !message.isBlank()) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("broken pipe")
                        || normalized.contains("connection reset by peer")
                        || normalized.contains("an established connection was aborted")
                        || message.contains("호스트 시스템의 소프트웨어에 의해 중단")
                        || message.contains("호스트 시스템의 소프트웨어의 의해 중단")
                        || message.contains("원격 호스트에 의해 강제로 끊")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    public record ApiErrorResponse(
            LocalDateTime timestamp,
            int status,
            String error,
            String code,
            String message,
            String path
    ) {
    }
}
