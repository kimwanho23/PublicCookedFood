package kwh.PublicCookedFood.account.facade;

import kwh.PublicCookedFood.account.audit.AccountAuditPublisher;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.dto.request.AccountUpdateDto;
import kwh.PublicCookedFood.account.error.AccountErrorCode;
import kwh.PublicCookedFood.account.facade.profile.AccountProfileViewStrategy;
import kwh.PublicCookedFood.account.policy.AccountProfileAccessPolicy;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.account.service.AccountService;
import kwh.PublicCookedFood.board.service.ImageService;
import kwh.PublicCookedFood.common.error.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountProfileFacadeUnitTest {

    @Mock
    private AccountService accountService;
    @Mock
    private AccountBlockService accountBlockService;
    @Mock
    private AccountAuditPublisher accountAuditPublisher;
    @Mock
    private AccountProfileAccessPolicy accountProfileAccessPolicy;
    @Mock
    private ImageService imageService;

    private AccountProfileFacade accountProfileFacade;

    @BeforeEach
    void setUp() {
        accountProfileFacade = new AccountProfileFacade(
                accountService,
                accountBlockService,
                accountAuditPublisher,
                accountProfileAccessPolicy,
                List.<AccountProfileViewStrategy>of(),
                imageService
        );
    }

    @Test
    void updateProfile_attachesNewImageAndCleansUpOldImageWhenChanged() {
        Account loginAccount = accountWithProfileImage(1L, "account@test.com", "/images/old-profile.jpg");
        Account existingAccount = accountWithProfileImage(1L, "account@test.com", "/images/old-profile.jpg");
        when(accountService.findAccountByEmail("account@test.com")).thenReturn(existingAccount);
        when(accountService.save(existingAccount)).thenReturn(existingAccount);

        AccountUpdateDto updateDto = new AccountUpdateDto();
        updateDto.setName("updated");
        updateDto.setPhoneNumber("010-1234-5678");
        updateDto.setBirthDate(LocalDate.of(1999, 1, 2));
        updateDto.setProfileImageUrl("/images/new-profile.jpg");

        AccountProfileFacade.ProfileUpdateResult result = accountProfileFacade.updateProfile(loginAccount, updateDto);

        assertThat(result.success()).isTrue();
        verify(imageService).attachProfileImageIfPresent("/images/new-profile.jpg");
        verify(imageService).cleanupImageByUrlIfUnlinked("/images/old-profile.jpg");
        verify(accountAuditPublisher).accountProfileUpdate(1L);
    }

    @Test
    void updateProfile_doesNotCleanupImageWhenProfileImageUnchanged() {
        Account loginAccount = accountWithProfileImage(1L, "account@test.com", "/images/same.jpg");
        Account existingAccount = accountWithProfileImage(1L, "account@test.com", "/images/same.jpg");
        when(accountService.findAccountByEmail("account@test.com")).thenReturn(existingAccount);
        when(accountService.save(existingAccount)).thenReturn(existingAccount);

        AccountUpdateDto updateDto = new AccountUpdateDto();
        updateDto.setName("same");
        updateDto.setProfileImageUrl("/images/same.jpg");

        AccountProfileFacade.ProfileUpdateResult result = accountProfileFacade.updateProfile(loginAccount, updateDto);

        assertThat(result.success()).isTrue();
        verify(imageService).attachProfileImageIfPresent("/images/same.jpg");
        verify(imageService, never()).cleanupImageByUrlIfUnlinked("/images/same.jpg");
    }

    @Test
    void updateProfile_whenSaveFails_returnsFailureWithoutSideEffects() {
        Account loginAccount = accountWithProfileImage(1L, "account@test.com", "/images/old.jpg");
        Account existingAccount = accountWithProfileImage(1L, "account@test.com", "/images/old.jpg");
        when(accountService.findAccountByEmail("account@test.com")).thenReturn(existingAccount);
        when(accountService.save(existingAccount)).thenThrow(new AppException(AccountErrorCode.ACCOUNT_NAME_DUPLICATED));

        AccountUpdateDto updateDto = new AccountUpdateDto();
        updateDto.setName("updated");
        updateDto.setProfileImageUrl("/images/new.jpg");

        AccountProfileFacade.ProfileUpdateResult result = accountProfileFacade.updateProfile(loginAccount, updateDto);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo(AccountErrorCode.ACCOUNT_NAME_DUPLICATED.message());
        verify(imageService, never()).attachProfileImageIfPresent("/images/new.jpg");
        verify(imageService, never()).cleanupImageByUrlIfUnlinked("/images/old.jpg");
        verify(accountAuditPublisher, never()).accountProfileUpdate(1L);
    }

    private Account accountWithProfileImage(Long id, String email, String profileImageUrl) {
        return Account.builder()
                .id(id)
                .email(email)
                .name("tester")
                .authority(Role.USER)
                .loginMethod("Current")
                .profileImageUrl(profileImageUrl)
                .build();
    }
}
