package kwh.PublicCookedFood.account.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.account.error.AccountRecoveryErrorCode;
import kwh.PublicCookedFood.account.exception.PasswordResetCodeDeliveryException;
import kwh.PublicCookedFood.account.domain.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountRecoveryVerificationService {

    private static final String PASSWORD_RESET_CODE_SESSION_KEY = "passwordResetCodeState";
    private static final long CODE_TTL_MILLIS = Duration.ofMinutes(10).toMillis();
    private static final int MAX_VERIFY_FAILURES = 5;

    private final AccountService accountService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final SecureRandom secureRandom = new SecureRandom();

    public boolean issuePasswordResetCode(HttpServletRequest request,
                                          String email,
                                          LocalDate birthDate,
                                          String phoneNumber) {
        if (request == null) {
            throw new IllegalStateException("요청 정보를 확인할 수 없습니다.");
        }

        HttpSession session = request.getSession(true);
        Optional<Account> accountOptional = accountService.findByEmailAndPhoneNumberAndBirthDate(email, phoneNumber, birthDate);
        if (accountOptional.isEmpty()) {
            clearPasswordResetCode(session);
            return false;
        }

        Account account = accountOptional.get();
        String code = generateVerificationCode();
        String codeHash = hashCode(code);
        long expiresAtMillis = System.currentTimeMillis() + CODE_TTL_MILLIS;

        PasswordResetCodeState state = new PasswordResetCodeState(
                normalizeText(account.getEmail()),
                birthDate,
                normalizePhone(phoneNumber),
                codeHash,
                expiresAtMillis,
                0
        );

        try {
            sendPasswordResetCodeMail(account.getEmail(), code);
            session.setAttribute(PASSWORD_RESET_CODE_SESSION_KEY, state);
            return true;
        } catch (PasswordResetCodeDeliveryException e) {
            clearPasswordResetCode(session);
            throw e;
        }
    }

    public void verifyPasswordResetCode(HttpServletRequest request,
                                        String email,
                                        LocalDate birthDate,
                                        String phoneNumber,
                                        String verificationCode) {
        if (request == null) {
            throw new IllegalStateException("요청 정보를 확인할 수 없습니다.");
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new AppException(AccountRecoveryErrorCode.PASSWORD_RESET_CODE_REQUIRED);
        }

        Object stateObject = session.getAttribute(PASSWORD_RESET_CODE_SESSION_KEY);
        if (!(stateObject instanceof PasswordResetCodeState state)) {
            throw new AppException(AccountRecoveryErrorCode.PASSWORD_RESET_CODE_REQUIRED);
        }

        if (state.isExpired()) {
            clearPasswordResetCode(session);
            throw new AppException(AccountRecoveryErrorCode.PASSWORD_RESET_CODE_EXPIRED);
        }

        if (!state.matchesIdentity(normalizeText(email), birthDate, normalizePhone(phoneNumber))) {
            throw new IllegalArgumentException("인증 코드 발급 시 입력한 계정 정보와 일치해야 합니다.");
        }

        String normalizedCode = normalizeText(verificationCode);
        if (normalizedCode == null || !state.codeHash().equals(hashCode(normalizedCode))) {
            int nextFailures = state.failedAttempts() + 1;
            if (nextFailures >= MAX_VERIFY_FAILURES) {
                clearPasswordResetCode(session);
                throw new IllegalArgumentException("인증 코드 입력 횟수를 초과했습니다. 코드를 다시 발급받아주세요.");
            }
            session.setAttribute(PASSWORD_RESET_CODE_SESSION_KEY, state.withFailedAttempts(nextFailures));
            throw new IllegalArgumentException("인증 코드가 올바르지 않습니다.");
        }

        if (state.failedAttempts() > 0) {
            session.setAttribute(PASSWORD_RESET_CODE_SESSION_KEY, state.withFailedAttempts(0));
        }
    }

    public void clearPasswordResetCode(HttpServletRequest request) {
        if (request == null) {
            return;
        }
        clearPasswordResetCode(request.getSession(false));
    }

    private void sendPasswordResetCodeMail(String toEmail, String code) {
        JavaMailSender javaMailSender = mailSenderProvider.getIfAvailable();
        if (javaMailSender == null) {
            throw new PasswordResetCodeDeliveryException(
                    AccountRecoveryErrorCode.PASSWORD_RESET_CODE_DELIVERY_FAILED.message(),
                    null
            );
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[PubCook] 비밀번호 재설정 인증 코드");
        message.setText("비밀번호 재설정 인증 코드는 [" + code + "] 입니다.\n"
                + "10분 이내에 입력해주세요.\n"
                + "요청하지 않으셨다면 이 메일은 무시해주세요.");
        try {
            javaMailSender.send(message);
        } catch (org.springframework.mail.MailException e) {
            throw new PasswordResetCodeDeliveryException(
                    AccountRecoveryErrorCode.PASSWORD_RESET_CODE_DELIVERY_FAILED.message(),
                    e
            );
        }
    }

    private String generateVerificationCode() {
        int number = secureRandom.nextInt(900000) + 100000;
        return Integer.toString(number);
    }

    private String hashCode(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(code.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("인증 코드 처리 중 오류가 발생했습니다.", e);
        }
    }

    private void clearPasswordResetCode(HttpSession session) {
        if (session == null) {
            return;
        }
        session.removeAttribute(PASSWORD_RESET_CODE_SESSION_KEY);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        String digits = phoneNumber.replaceAll("[^0-9]", "");
        return digits.isBlank() ? null : digits;
    }

    private record PasswordResetCodeState(
            String email,
            LocalDate birthDate,
            String phoneNumber,
            String codeHash,
            long expiresAtMillis,
            int failedAttempts
    ) implements Serializable {
        private boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMillis;
        }

        private boolean matchesIdentity(String email, LocalDate birthDate, String phoneNumber) {
            return equalsOrEmpty(this.email, email)
                    && equalsBirthDate(this.birthDate, birthDate)
                    && equalsOrEmpty(this.phoneNumber, phoneNumber);
        }

        private PasswordResetCodeState withFailedAttempts(int nextFailures) {
            return new PasswordResetCodeState(
                    email,
                    birthDate,
                    phoneNumber,
                    codeHash,
                    expiresAtMillis,
                    nextFailures
            );
        }

        private boolean equalsOrEmpty(String left, String right) {
            if (left == null || right == null) {
                return false;
            }
            return left.equals(right);
        }

        private boolean equalsBirthDate(LocalDate left, LocalDate right) {
            if (left == null || right == null) {
                return false;
            }
            return left.equals(right);
        }
    }
}

