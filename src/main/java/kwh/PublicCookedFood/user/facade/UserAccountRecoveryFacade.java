package kwh.PublicCookedFood.user.facade;

import jakarta.servlet.http.HttpServletRequest;
import kwh.PublicCookedFood.config.AuthThrottleService;
import kwh.PublicCookedFood.user.audit.AccountRecoveryAuditPublisher;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.policy.UserAccountRecoveryPolicy;
import kwh.PublicCookedFood.user.service.AccountRecoveryVerificationService;
import kwh.PublicCookedFood.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAccountRecoveryFacade {

    private final UserService userService;
    private final AccountRecoveryAuditPublisher accountRecoveryAuditPublisher;
    private final PasswordEncoder passwordEncoder;
    private final AuthThrottleService authThrottleService;
    private final AccountRecoveryVerificationService accountRecoveryVerificationService;
    private final UserAccountRecoveryPolicy userAccountRecoveryPolicy;

    public String resolveActiveTab(String tab) {
        return userAccountRecoveryPolicy.resolveActiveTab(tab);
    }

    @Transactional(readOnly = true)
    public RecoveryOperationResult findAccountEmail(HttpServletRequest request,
                                                    String name,
                                                    String phoneNumber) {
        String validationMessage = userAccountRecoveryPolicy.validateFindEmailRequest(name, phoneNumber);
        if (validationMessage != null) {
            return RecoveryOperationResult.failure(validationMessage);
        }

        String throttleIdentity = (name == null ? "" : name.trim()) + "|" + (phoneNumber == null ? "" : phoneNumber.trim());
        String throttledMessage = throttleIfNeeded(request, "find-email", throttleIdentity);
        if (throttledMessage != null) {
            return RecoveryOperationResult.failure(throttledMessage);
        }

        Users foundUser = userService.findByNameAndPhoneNumber(name, phoneNumber).orElse(null);
        if (foundUser == null) {
            return onFindEmailNotFound();
        }
        return onFindEmailSuccess(foundUser.getId(), foundUser.getEmail());
    }

    @Transactional
    public RecoveryOperationResult requestPasswordResetCode(HttpServletRequest request,
                                                            String email,
                                                            String name,
                                                            String phoneNumber) {
        String validationMessage = userAccountRecoveryPolicy.validateResetCodeRequest(email, name, phoneNumber);
        if (validationMessage != null) {
            return RecoveryOperationResult.failure(validationMessage);
        }

        String throttledMessage = throttleIfNeeded(request, "reset-password-code", email);
        if (throttledMessage != null) {
            return RecoveryOperationResult.failure(throttledMessage);
        }

        boolean issued;
        try {
            issued = accountRecoveryVerificationService.issuePasswordResetCode(request, email, name, phoneNumber);
        } catch (IllegalStateException e) {
            issued = false;
            accountRecoveryAuditPublisher.resetPasswordCodeMailUnavailable(email, e);
        }

        if (issued) {
            accountRecoveryAuditPublisher.resetPasswordCodeIssued(email);
        } else {
            accountRecoveryAuditPublisher.resetPasswordCodeNotFoundOrSkipped(email);
        }

        return RecoveryOperationResult.success("입력하신 이메일로 인증 코드를 전송했습니다. 코드를 확인 후 비밀번호를 재설정해주세요.");
    }

    @Transactional
    public RecoveryOperationResult resetAccountPassword(HttpServletRequest request,
                                                        String email,
                                                        String name,
                                                        String phoneNumber,
                                                        String verificationCode,
                                                        String newPassword,
                                                        String confirmPassword) {
        String validationMessage = userAccountRecoveryPolicy.validateResetPasswordRequest(
                email,
                name,
                phoneNumber,
                verificationCode,
                newPassword,
                confirmPassword
        );
        if (validationMessage != null) {
            return RecoveryOperationResult.failure(validationMessage);
        }

        String verifyThrottledMessage = throttleIfNeeded(request, "reset-password-verify", email);
        if (verifyThrottledMessage != null) {
            return RecoveryOperationResult.failure(verifyThrottledMessage);
        }

        try {
            accountRecoveryVerificationService.verifyPasswordResetCode(
                    request,
                    email,
                    name,
                    phoneNumber,
                    verificationCode
            );
        } catch (IllegalArgumentException | IllegalStateException e) {
            return RecoveryOperationResult.failure(e.getMessage());
        }

        String resetThrottledMessage = throttleIfNeeded(request, "reset-password", email);
        if (resetThrottledMessage != null) {
            return RecoveryOperationResult.failure(resetThrottledMessage);
        }

        Users foundUser = userService.findByEmailAndNameAndPhoneNumber(email, name, phoneNumber).orElse(null);
        if (foundUser == null) {
            return onResetPasswordNotFound();
        }
        return onResetPasswordSuccess(request, email, newPassword, foundUser);
    }

    private String throttleIfNeeded(HttpServletRequest request,
                                    String action,
                                    String identity) {
        if (authThrottleService.tryConsumeRecoveryAttempt(request, action, identity)) {
            return null;
        }
        long retryAfter = authThrottleService.getRecoveryRetryAfterSeconds(request, action, identity);
        return userAccountRecoveryPolicy.buildThrottleMessage(retryAfter);
    }

    private RecoveryOperationResult onFindEmailSuccess(Long userId, String userEmail) {
        accountRecoveryAuditPublisher.findEmailSuccess(userId);
        return RecoveryOperationResult.successWithFoundEmail(
                "가입된 이메일을 확인했습니다.",
                userAccountRecoveryPolicy.maskEmail(userEmail)
        );
    }

    private RecoveryOperationResult onFindEmailNotFound() {
        accountRecoveryAuditPublisher.findEmailNotFound();
        return RecoveryOperationResult.failure("일치하는 가입 정보가 없습니다.");
    }

    private RecoveryOperationResult onResetPasswordSuccess(HttpServletRequest request,
                                                           String email,
                                                           String newPassword,
                                                           Users user) {
        user.updatePassword(passwordEncoder.encode(newPassword));
        userService.save(user);
        authThrottleService.clearLoginFailures(request, email);
        accountRecoveryAuditPublisher.resetPasswordSuccess(user.getId());
        return RecoveryOperationResult.success("비밀번호가 재설정되었습니다. 새 비밀번호로 로그인해주세요.");
    }

    private RecoveryOperationResult onResetPasswordNotFound() {
        accountRecoveryAuditPublisher.resetPasswordNotFound();
        return RecoveryOperationResult.failure("일치하는 가입 정보가 없습니다.");
    }

    public record RecoveryOperationResult(boolean success,
                                          String message,
                                          String foundEmail) {

        public static RecoveryOperationResult success(String message) {
            return new RecoveryOperationResult(true, message, null);
        }

        public static RecoveryOperationResult successWithFoundEmail(String message,
                                                                    String foundEmail) {
            return new RecoveryOperationResult(true, message, foundEmail);
        }

        public static RecoveryOperationResult failure(String message) {
            return new RecoveryOperationResult(false, message, null);
        }
    }
}
