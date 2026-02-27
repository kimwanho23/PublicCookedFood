package kwh.PublicCookedFood.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import kwh.PublicCookedFood.recipeSaveLogic.RecipeImportException;
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                         HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, buildValidationMessage(e.getBindingResult()), request);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBindException(BindException e, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, buildValidationMessage(e.getBindingResult()), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException e,
                                                                      HttpServletRequest request) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, message.isBlank() ? "요청 값이 유효하지 않습니다." : message, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                                               HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "요청 값의 타입이 올바르지 않습니다.", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException e,
                                                                  HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, defaultMessage(e.getMessage(), "잘못된 요청입니다."), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException e, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, defaultMessage(e.getMessage(), "요청을 처리할 수 없습니다."), request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException e,
                                                                HttpServletRequest request) {
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE,
                "업로드 가능한 최대 파일 크기를 초과했습니다.", request);
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiErrorResponse> handleStorageException(StorageException e, HttpServletRequest request) {
        log.error("Storage exception. uri={}", request.getRequestURI(), e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                defaultMessage(e.getMessage(), "파일 저장 중 오류가 발생했습니다."), request);
    }

    @ExceptionHandler(RecipeImportException.class)
    public ResponseEntity<ApiErrorResponse> handleRecipeImportException(RecipeImportException e,
                                                                        HttpServletRequest request) {
        log.error("Recipe import exception. uri={}", request.getRequestURI(), e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                defaultMessage(e.getMessage(), "레시피 데이터 동기화 중 오류가 발생했습니다."), request);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiErrorResponse> handleNoSuchElement(NoSuchElementException e, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다.", request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException e,
                                                                          HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }
        return buildResponse(status, defaultMessage(e.getReason(), status.getReasonPhrase()), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpected(Exception e, HttpServletRequest request) {
        if (isClientAbortException(e)) {
            log.debug("Client disconnected during response write. uri={}", request.getRequestURI());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        log.error("Unhandled exception. uri={}", request.getRequestURI(), e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", request);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status,
                                                           String message,
                                                           HttpServletRequest request) {
        return ResponseEntity.status(status).body(
                new ApiErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        request.getRequestURI()
                )
        );
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
            String message,
            String path
    ) {
    }
}
