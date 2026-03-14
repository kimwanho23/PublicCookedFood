package kwh.PublicCookedFood.account.facade;

import kwh.PublicCookedFood.account.audit.AccountAuditPublisher;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Gender;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.dto.request.AccountSaveDto;
import kwh.PublicCookedFood.account.error.AccountErrorCode;
import kwh.PublicCookedFood.account.service.AccountService;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.storage.ImageLifecycleService;
import kwh.PublicCookedFood.storage.ImageUrls;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountAuthFacadeUnitTest {

    @Mock
    private AccountService accountService;
    @Mock
    private AccountAuditPublisher accountAuditPublisher;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ImageLifecycleService imageLifecycleService;

    @InjectMocks
    private AccountAuthFacade accountAuthFacade;

    @Test
    void signup_attachesProfileImageWhenSignupSucceeds() {
        AccountSaveDto accountSaveDto = createAccountSaveDto("/images/signup-profile.jpg");
        Account savedAccount = Account.builder()
                .id(11L)
                .email("signup@test.com")
                .name("signup-account")
                .authority(Role.USER)
                .loginMethod("Current")
                .profileImageUrl("/images/signup-profile.jpg")
                .build();

        when(passwordEncoder.encode("password1234")).thenReturn("encoded-password");
        when(accountService.save(any(Account.class))).thenReturn(savedAccount);

        AccountAuthFacade.SignupResult result = accountAuthFacade.signup(accountSaveDto);

        assertThat(result.success()).isTrue();
        verify(imageLifecycleService).attachImagesIfPresent(ImageUrls.single("/images/signup-profile.jpg"));
        verify(accountAuditPublisher).accountSignup(11L, "signup@test.com");
    }

    @Test
    void signup_whenSaveFails_doesNotAttachProfileImage() {
        AccountSaveDto accountSaveDto = createAccountSaveDto("/images/signup-profile.jpg");

        when(passwordEncoder.encode("password1234")).thenReturn("encoded-password");
        when(accountService.save(any(Account.class))).thenThrow(new AppException(AccountErrorCode.ACCOUNT_EMAIL_DUPLICATED));

        AccountAuthFacade.SignupResult result = accountAuthFacade.signup(accountSaveDto);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo(AccountErrorCode.ACCOUNT_EMAIL_DUPLICATED.message());
        verify(imageLifecycleService, never()).attachImagesIfPresent(ImageUrls.single("/images/signup-profile.jpg"));
        verify(accountAuditPublisher, never()).accountSignup(anyLong(), anyString());
    }

    @Test
    void signup_whenNameIsDuplicated_returnsNicknameErrorMessage() {
        AccountSaveDto accountSaveDto = createAccountSaveDto("/images/signup-profile.jpg");

        when(passwordEncoder.encode("password1234")).thenReturn("encoded-password");
        when(accountService.save(any(Account.class))).thenThrow(new AppException(AccountErrorCode.ACCOUNT_NAME_DUPLICATED));

        AccountAuthFacade.SignupResult result = accountAuthFacade.signup(accountSaveDto);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo(AccountErrorCode.ACCOUNT_NAME_DUPLICATED.message());
        verify(imageLifecycleService, never()).attachImagesIfPresent(ImageUrls.single("/images/signup-profile.jpg"));
        verify(accountAuditPublisher, never()).accountSignup(anyLong(), anyString());
    }

    private AccountSaveDto createAccountSaveDto(String profileImageUrl) {
        AccountSaveDto accountSaveDto = new AccountSaveDto();
        accountSaveDto.setEmail("signup@test.com");
        accountSaveDto.setPassword("password1234");
        accountSaveDto.setName("signup-account");
        accountSaveDto.setPhoneNumber("010-1234-5678");
        accountSaveDto.setBirthDate(LocalDate.of(1995, 5, 5));
        accountSaveDto.setGender(Gender.MALE);
        accountSaveDto.setProfileImageUrl(profileImageUrl);
        return accountSaveDto;
    }
}
