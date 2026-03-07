package kwh.PublicCookedFood.account.facade;

import kwh.PublicCookedFood.config.AuthThrottleService;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.account.error.AccountRecoveryErrorCode;
import kwh.PublicCookedFood.account.audit.AccountRecoveryAuditPublisher;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.exception.PasswordResetCodeDeliveryException;
import kwh.PublicCookedFood.account.policy.AccountRecoveryPolicy;
import kwh.PublicCookedFood.account.service.AccountRecoveryVerificationService;
import kwh.PublicCookedFood.account.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountRecoveryFacadeUnitTest {

    @Mock
    private AccountService accountService;

    @Mock
    private AccountRecoveryAuditPublisher accountRecoveryAuditPublisher;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthThrottleService authThrottleService;

    @Mock
    private AccountRecoveryVerificationService accountRecoveryVerificationService;

    @Mock
    private AccountRecoveryPolicy accountRecoveryPolicy;

    @InjectMocks
    private AccountRecoveryFacade accountRecoveryFacade;

    @Test
    void requestPasswordResetCode_returnsFailureWhenMailDeliveryFails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String birthDate = "1990-01-02";
        PasswordResetCodeDeliveryException exception =
                new PasswordResetCodeDeliveryException("인증 코드 발송에 실패했습니다. 잠시 후 다시 시도해주세요.", new IllegalStateException("smtp-down"));
        when(accountRecoveryPolicy.validateResetCodeRequest("recover@test.com", birthDate, "010-1234-5678"))
                .thenReturn(null);
        when(accountRecoveryPolicy.parseBirthDate(birthDate)).thenReturn(LocalDate.of(1990, 1, 2));
        when(authThrottleService.tryConsumeRecoveryAttempt(request, "reset-password-code", "recover@test.com"))
                .thenReturn(true);
        when(accountRecoveryVerificationService.issuePasswordResetCode(
                request,
                "recover@test.com",
                LocalDate.of(1990, 1, 2),
                "010-1234-5678"
        )).thenThrow(exception);

        AccountRecoveryFacade.RecoveryOperationResult result =
                accountRecoveryFacade.requestPasswordResetCode(request, "recover@test.com", birthDate, "010-1234-5678");

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("인증 코드 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        verify(accountRecoveryAuditPublisher).resetPasswordCodeMailUnavailable("recover@test.com", exception);
        verify(accountRecoveryAuditPublisher, never()).resetPasswordCodeIssued(anyString());
        verify(accountRecoveryAuditPublisher, never()).resetPasswordCodeNotFoundOrSkipped(anyString());
    }

    @Test
    void resetAccountPassword_doesNotClearCodeWhenFinalThrottleBlocks() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String birthDate = "1990-01-02";
        when(accountRecoveryPolicy.validateResetPasswordRequest(
                "recover@test.com",
                birthDate,
                "010-1234-5678",
                "123456",
                "password1234",
                "password1234"
        )).thenReturn(null);
        when(accountRecoveryPolicy.parseBirthDate(birthDate)).thenReturn(LocalDate.of(1990, 1, 2));
        when(authThrottleService.tryConsumeRecoveryAttempt(request, "reset-password-verify", "recover@test.com"))
                .thenReturn(true);
        when(authThrottleService.tryConsumeRecoveryAttempt(request, "reset-password", "recover@test.com"))
                .thenReturn(false);
        when(authThrottleService.getRecoveryRetryAfterSeconds(request, "reset-password", "recover@test.com"))
                .thenReturn(42L);
        when(accountRecoveryPolicy.buildThrottleMessage(42L))
                .thenReturn("요청이 너무 많습니다. 약 42초 후 다시 시도해주세요.");

        AccountRecoveryFacade.RecoveryOperationResult result =
                accountRecoveryFacade.resetAccountPassword(
                        request,
                        "recover@test.com",
                        birthDate,
                        "010-1234-5678",
                        "123456",
                        "password1234",
                        "password1234"
                );

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("요청이 너무 많습니다. 약 42초 후 다시 시도해주세요.");
        verify(accountRecoveryVerificationService).verifyPasswordResetCode(
                request,
                "recover@test.com",
                LocalDate.of(1990, 1, 2),
                "010-1234-5678",
                "123456"
        );
        verify(accountRecoveryVerificationService, never()).clearPasswordResetCode(any());
        verify(accountService, never()).findByEmailAndPhoneNumberAndBirthDate(anyString(), anyString(), any());
    }

    @Test
    void resetAccountPassword_clearsCodeAfterPasswordResetSucceeds() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Account account = createAccount(10L, "recover@test.com");
        String birthDate = "1990-01-02";
        when(accountRecoveryPolicy.validateResetPasswordRequest(
                "recover@test.com",
                birthDate,
                "010-1234-5678",
                "123456",
                "password1234",
                "password1234"
        )).thenReturn(null);
        when(accountRecoveryPolicy.parseBirthDate(birthDate)).thenReturn(LocalDate.of(1990, 1, 2));
        when(authThrottleService.tryConsumeRecoveryAttempt(request, "reset-password-verify", "recover@test.com"))
                .thenReturn(true);
        when(authThrottleService.tryConsumeRecoveryAttempt(request, "reset-password", "recover@test.com"))
                .thenReturn(true);
        doNothing().when(accountRecoveryVerificationService).verifyPasswordResetCode(
                request,
                "recover@test.com",
                LocalDate.of(1990, 1, 2),
                "010-1234-5678",
                "123456"
        );
        when(accountService.findByEmailAndPhoneNumberAndBirthDate("recover@test.com", "010-1234-5678", LocalDate.of(1990, 1, 2)))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.encode("password1234")).thenReturn("encoded-password");
        when(accountService.save(account)).thenReturn(account);

        AccountRecoveryFacade.RecoveryOperationResult result =
                accountRecoveryFacade.resetAccountPassword(
                        request,
                        "recover@test.com",
                        birthDate,
                        "010-1234-5678",
                        "123456",
                        "password1234",
                        "password1234"
                );

        assertThat(result.success()).isTrue();
        verify(accountRecoveryVerificationService).clearPasswordResetCode(request);
        verify(accountRecoveryAuditPublisher).resetPasswordSuccess(10L);
    }

    @Test
    void resetAccountPassword_returnsFailureWhenVerificationCodeWasNotIssued() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String birthDate = "1990-01-02";
        when(accountRecoveryPolicy.validateResetPasswordRequest(
                "recover@test.com",
                birthDate,
                "010-1234-5678",
                "123456",
                "password1234",
                "password1234"
        )).thenReturn(null);
        when(accountRecoveryPolicy.parseBirthDate(birthDate)).thenReturn(LocalDate.of(1990, 1, 2));
        when(authThrottleService.tryConsumeRecoveryAttempt(request, "reset-password-verify", "recover@test.com"))
                .thenReturn(true);
        doThrow(new AppException(AccountRecoveryErrorCode.PASSWORD_RESET_CODE_REQUIRED))
                .when(accountRecoveryVerificationService)
                .verifyPasswordResetCode(request, "recover@test.com", LocalDate.of(1990, 1, 2), "010-1234-5678", "123456");

        AccountRecoveryFacade.RecoveryOperationResult result =
                accountRecoveryFacade.resetAccountPassword(
                        request,
                        "recover@test.com",
                        birthDate,
                        "010-1234-5678",
                        "123456",
                        "password1234",
                        "password1234"
                );

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo(AccountRecoveryErrorCode.PASSWORD_RESET_CODE_REQUIRED.message());
        verify(accountService, never()).findByEmailAndPhoneNumberAndBirthDate(anyString(), anyString(), any());
    }

    private Account createAccount(Long id, String email) {
        return Account.builder()
                .id(id)
                .email(email)
                .name("홍길동")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }
}
