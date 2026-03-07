package kwh.PublicCookedFood.account.service;

import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.error.AccountRecoveryErrorCode;
import kwh.PublicCookedFood.account.exception.PasswordResetCodeDeliveryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountRecoveryVerificationServiceUnitTest {

    private static final String PASSWORD_RESET_CODE_SESSION_KEY = "passwordResetCodeState";
    private static final Pattern VERIFICATION_CODE_PATTERN = Pattern.compile("\\[(\\d{6})\\]");

    @Mock
    private AccountService accountService;

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private AccountRecoveryVerificationService accountRecoveryVerificationService;

    @Test
    void verifyPasswordResetCode_keepsCodeStateUntilResetSucceeds() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Account account = createAccount(1L, "recover@test.com");
        LocalDate birthDate = LocalDate.of(1990, 1, 2);
        when(accountService.findByEmailAndPhoneNumberAndBirthDate("recover@test.com", "010-1234-5678", birthDate))
                .thenReturn(Optional.of(account));
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);

        accountRecoveryVerificationService.issuePasswordResetCode(
                request,
                "recover@test.com",
                birthDate,
                "010-1234-5678"
        );

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(mailCaptor.capture());
        String verificationCode = extractVerificationCode(mailCaptor.getValue().getText());

        accountRecoveryVerificationService.verifyPasswordResetCode(
                request,
                "recover@test.com",
                birthDate,
                "010-1234-5678",
                verificationCode
        );

        assertThat(request.getSession(false)).isNotNull();
        assertThat(request.getSession(false).getAttribute(PASSWORD_RESET_CODE_SESSION_KEY)).isNotNull();

        accountRecoveryVerificationService.clearPasswordResetCode(request);

        assertThat(request.getSession(false).getAttribute(PASSWORD_RESET_CODE_SESSION_KEY)).isNull();
    }

    @Test
    void issuePasswordResetCode_throwsDeliveryExceptionWhenMailSendFails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Account account = createAccount(2L, "recover2@test.com");
        LocalDate birthDate = LocalDate.of(1992, 2, 3);
        when(accountService.findByEmailAndPhoneNumberAndBirthDate("recover2@test.com", "010-2222-3333", birthDate))
                .thenReturn(Optional.of(account));
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        doThrow(new MailSendException("smtp-down")).when(javaMailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> accountRecoveryVerificationService.issuePasswordResetCode(
                request,
                "recover2@test.com",
                birthDate,
                "010-2222-3333"
        ))
                .isInstanceOf(PasswordResetCodeDeliveryException.class)
                .hasMessageContaining("인증 코드 발송에 실패했습니다");

        assertThat(request.getSession(false)).isNotNull();
        assertThat(request.getSession(false).getAttribute(PASSWORD_RESET_CODE_SESSION_KEY)).isNull();
    }

    @Test
    void verifyPasswordResetCode_throwsAppExceptionWhenCodeWasNotIssued() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> accountRecoveryVerificationService.verifyPasswordResetCode(
                request,
                "recover@test.com",
                LocalDate.of(1990, 1, 2),
                "010-1234-5678",
                "123456"
        ))
                .isInstanceOfSatisfying(AppException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AccountRecoveryErrorCode.PASSWORD_RESET_CODE_REQUIRED));
    }

    @Test
    void verifyPasswordResetCode_throwsAppExceptionWhenCodeIsExpired() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Account account = createAccount(3L, "recover3@test.com");
        LocalDate birthDate = LocalDate.of(1993, 3, 4);
        when(accountService.findByEmailAndPhoneNumberAndBirthDate("recover3@test.com", "010-9999-8888", birthDate))
                .thenReturn(Optional.of(account));
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);

        accountRecoveryVerificationService.issuePasswordResetCode(
                request,
                "recover3@test.com",
                birthDate,
                "010-9999-8888"
        );

        Object stateObject = request.getSession(false).getAttribute(PASSWORD_RESET_CODE_SESSION_KEY);
        java.lang.reflect.RecordComponent[] components = stateObject.getClass().getRecordComponents();
        java.lang.reflect.Method emailAccessor = findAccessor(components, "email");
        java.lang.reflect.Method birthDateAccessor = findAccessor(components, "birthDate");
        java.lang.reflect.Method phoneAccessor = findAccessor(components, "phoneNumber");
        java.lang.reflect.Method codeHashAccessor = findAccessor(components, "codeHash");
        java.lang.reflect.Constructor<?> constructor = stateObject.getClass().getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        try {
            Object expiredState = constructor.newInstance(
                    emailAccessor.invoke(stateObject),
                    birthDateAccessor.invoke(stateObject),
                    phoneAccessor.invoke(stateObject),
                    codeHashAccessor.invoke(stateObject),
                    System.currentTimeMillis() - 1,
                    0
            );
            request.getSession(false).setAttribute(PASSWORD_RESET_CODE_SESSION_KEY, expiredState);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }

        assertThatThrownBy(() -> accountRecoveryVerificationService.verifyPasswordResetCode(
                request,
                "recover3@test.com",
                birthDate,
                "010-9999-8888",
                "123456"
        ))
                .isInstanceOfSatisfying(AppException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AccountRecoveryErrorCode.PASSWORD_RESET_CODE_EXPIRED));

        assertThat(request.getSession(false).getAttribute(PASSWORD_RESET_CODE_SESSION_KEY)).isNull();
    }

    private java.lang.reflect.Method findAccessor(java.lang.reflect.RecordComponent[] components, String name) {
        for (java.lang.reflect.RecordComponent component : components) {
            if (component.getName().equals(name)) {
                return component.getAccessor();
            }
        }
        throw new AssertionError("Missing record component: " + name);
    }

    private String extractVerificationCode(String message) {
        assertThat(message).isNotBlank();
        Matcher matcher = VERIFICATION_CODE_PATTERN.matcher(message);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
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
