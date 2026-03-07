package kwh.PublicCookedFood.account.facade;

import jakarta.servlet.http.HttpServletRequest;
import kwh.PublicCookedFood.config.AuthThrottleService;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.ErrorMessageResolver;
import kwh.PublicCookedFood.account.audit.AccountRecoveryAuditPublisher;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.exception.PasswordResetCodeDeliveryException;
import kwh.PublicCookedFood.account.policy.AccountRecoveryPolicy;
import kwh.PublicCookedFood.account.service.AccountRecoveryVerificationService;
import kwh.PublicCookedFood.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AccountRecoveryFacade {

    private final AccountService accountService;
    private final AccountRecoveryAuditPublisher accountRecoveryAuditPublisher;
    private final PasswordEncoder passwordEncoder;
    private final AuthThrottleService authThrottleService;
    private final AccountRecoveryVerificationService accountRecoveryVerificationService;
    private final AccountRecoveryPolicy accountRecoveryPolicy;

    public String resolveActiveTab(String tab) {
        return accountRecoveryPolicy.resolveActiveTab(tab);
    }

    @Transactional(readOnly = true)
    public RecoveryOperationResult findAccountEmail(HttpServletRequest request,
                                                    String birthDate,
                                                    String phoneNumber) {
        String validationMessage = accountRecoveryPolicy.validateFindEmailRequest(birthDate, phoneNumber);
        if (validationMessage != null) {
            return RecoveryOperationResult.failure(validationMessage);
        }

        LocalDate parsedBirthDate = accountRecoveryPolicy.parseBirthDate(birthDate);
        String throttleIdentity = (birthDate == null ? "" : birthDate.trim()) + "|" + (phoneNumber == null ? "" : phoneNumber.trim());
        String throttledMessage = throttleIfNeeded(request, "find-email", throttleIdentity);
        if (throttledMessage != null) {
            return RecoveryOperationResult.failure(throttledMessage);
        }

        Account foundUser = accountService.findByPhoneNumberAndBirthDate(phoneNumber, parsedBirthDate).orElse(null);
        if (foundUser == null) {
            return onFindEmailNotFound();
        }
        return onFindEmailSuccess(foundUser.getId(), foundUser.getEmail());
    }

    @Transactional
    public RecoveryOperationResult requestPasswordResetCode(HttpServletRequest request,
                                                            String email,
                                                            String birthDate,
                                                            String phoneNumber) {
        String validationMessage = accountRecoveryPolicy.validateResetCodeRequest(email, birthDate, phoneNumber);
        if (validationMessage != null) {
            return RecoveryOperationResult.failure(validationMessage);
        }

        LocalDate parsedBirthDate = accountRecoveryPolicy.parseBirthDate(birthDate);
        String throttledMessage = throttleIfNeeded(request, "reset-password-code", email);
        if (throttledMessage != null) {
            return RecoveryOperationResult.failure(throttledMessage);
        }

        try {
            boolean issued = accountRecoveryVerificationService.issuePasswordResetCode(request, email, parsedBirthDate, phoneNumber);
            if (issued) {
                accountRecoveryAuditPublisher.resetPasswordCodeIssued(email);
            } else {
                accountRecoveryAuditPublisher.resetPasswordCodeNotFoundOrSkipped(email);
            }
            return RecoveryOperationResult.success("입력하신 이메일로 인증 코드를 발송했습니다. 코드를 확인해 비밀번호를 재설정해주세요.");
        } catch (PasswordResetCodeDeliveryException e) {
            accountRecoveryAuditPublisher.resetPasswordCodeMailUnavailable(email, e);
            return RecoveryOperationResult.failure(
                    ErrorMessageResolver.resolve(e, "인증 코드 발송에 실패했습니다. 잠시 후 다시 시도해주세요.")
            );
        }
    }

    @Transactional
    public RecoveryOperationResult resetAccountPassword(HttpServletRequest request,
                                                        String email,
                                                        String birthDate,
                                                        String phoneNumber,
                                                        String verificationCode,
                                                        String newPassword,
                                                        String confirmPassword) {
        String validationMessage = accountRecoveryPolicy.validateResetPasswordRequest(
                email,
                birthDate,
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

        LocalDate parsedBirthDate = accountRecoveryPolicy.parseBirthDate(birthDate);
        try {
            accountRecoveryVerificationService.verifyPasswordResetCode(
                    request,
                    email,
                    parsedBirthDate,
                    phoneNumber,
                    verificationCode
            );
        } catch (AppException | IllegalArgumentException e) {
            return RecoveryOperationResult.failure(
                    ErrorMessageResolver.resolve(e, "비밀번호 재설정 인증을 확인하지 못했습니다.")
            );
        }

        String resetThrottledMessage = throttleIfNeeded(request, "reset-password", email);
        if (resetThrottledMessage != null) {
            return RecoveryOperationResult.failure(resetThrottledMessage);
        }

        Account foundUser = accountService.findByEmailAndPhoneNumberAndBirthDate(email, phoneNumber, parsedBirthDate).orElse(null);
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
        return accountRecoveryPolicy.buildThrottleMessage(retryAfter);
    }

    private RecoveryOperationResult onFindEmailSuccess(Long accountId, String accountEmail) {
        accountRecoveryAuditPublisher.findEmailSuccess(accountId);
        return RecoveryOperationResult.successWithFoundEmail(
                "가입한 이메일을 확인했습니다.",
                accountRecoveryPolicy.maskEmail(accountEmail)
        );
    }

    private RecoveryOperationResult onFindEmailNotFound() {
        accountRecoveryAuditPublisher.findEmailNotFound();
        return RecoveryOperationResult.failure("일치하는 가입 정보가 없습니다.");
    }

    private RecoveryOperationResult onResetPasswordSuccess(HttpServletRequest request,
                                                           String email,
                                                           String newPassword,
                                                           Account account) {
        account.updatePassword(passwordEncoder.encode(newPassword));
        accountService.save(account);
        accountRecoveryVerificationService.clearPasswordResetCode(request);
        authThrottleService.clearLoginFailures(request, email);
        accountRecoveryAuditPublisher.resetPasswordSuccess(account.getId());
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

