package kwh.PublicCookedFood.account.policy;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

@Component
public class AccountRecoveryPolicy {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern VERIFICATION_CODE_PATTERN = Pattern.compile("^\\d{6}$");

    private static final String TAB_FIND_EMAIL = "find-email";
    private static final String TAB_RESET_PASSWORD = "reset-password";

    public String resolveActiveTab(String tab) {
        return TAB_RESET_PASSWORD.equals(tab) ? TAB_RESET_PASSWORD : TAB_FIND_EMAIL;
    }

    public String validateFindEmailRequest(String birthDate, String phoneNumber) {
        if (isBlank(birthDate) || isBlank(phoneNumber)) {
            return "생년월일과 전화번호를 모두 입력해주세요.";
        }
        if (!isValidBirthDate(birthDate)) {
            return "생년월일 형식이 올바르지 않습니다.";
        }
        return null;
    }

    public String validateResetCodeRequest(String email, String birthDate, String phoneNumber) {
        if (isBlank(email) || isBlank(birthDate) || isBlank(phoneNumber)) {
            return "이메일, 생년월일, 전화번호를 모두 입력해주세요.";
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return "이메일 형식이 올바르지 않습니다.";
        }
        if (!isValidBirthDate(birthDate)) {
            return "생년월일 형식이 올바르지 않습니다.";
        }
        return null;
    }

    public String validateResetPasswordRequest(String email,
                                               String birthDate,
                                               String phoneNumber,
                                               String verificationCode,
                                               String newPassword,
                                               String confirmPassword) {
        if (isBlank(email) || isBlank(birthDate) || isBlank(phoneNumber)
                || isBlank(verificationCode)
                || isBlank(newPassword) || isBlank(confirmPassword)) {
            return "모든 항목을 입력해주세요.";
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return "이메일 형식이 올바르지 않습니다.";
        }
        if (!isValidBirthDate(birthDate)) {
            return "생년월일 형식이 올바르지 않습니다.";
        }
        if (newPassword.length() < 8 || newPassword.length() > 50) {
            return "비밀번호는 8자 이상 50자 이하여야 합니다.";
        }
        if (!newPassword.equals(confirmPassword)) {
            return "새 비밀번호 확인이 일치하지 않습니다.";
        }
        if (!VERIFICATION_CODE_PATTERN.matcher(verificationCode.trim()).matches()) {
            return "인증 코드는 6자리 숫자여야 합니다.";
        }
        return null;
    }

    public LocalDate parseBirthDate(String birthDate) {
        if (isBlank(birthDate)) {
            return null;
        }
        try {
            return LocalDate.parse(birthDate.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public String buildThrottleMessage(long retryAfterSeconds) {
        if (retryAfterSeconds <= 0) {
            return "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.";
        }
        return "요청이 너무 많습니다. 약 " + retryAfterSeconds + "초 후 다시 시도해주세요.";
    }

    public String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        String id = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (id.length() <= 2) {
            return id.charAt(0) + "*" + domain;
        }
        return id.substring(0, 2) + "*".repeat(Math.max(1, id.length() - 2)) + domain;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isValidBirthDate(String birthDate) {
        return parseBirthDate(birthDate) != null;
    }
}
