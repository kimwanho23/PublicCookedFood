package kwh.PublicCookedFood.config.oauth2;

import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.error.AccountErrorCode;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.account.service.AccountNicknameGenerator;
import kwh.PublicCookedFood.account.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2AccountServiceUnitTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountNicknameGenerator accountNicknameGenerator;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private CustomOAuth2AccountService customOAuth2AccountService;

    @Test
    void saveOrUpdate_keepsExistingNicknameWhenEmailAlreadyExists() {
        OAuthAttributes attributes = OAuthAttributes.builder()
                .name("existing-oauth")
                .email("oauth@test.com")
                .emailVerified(true)
                .loginMethod("google")
                .attributes(Map.of())
                .nameAttributeKey("sub")
                .build();
        Account existingAccount = Account.builder()
                .id(1L)
                .email("oauth@test.com")
                .name("oauth-account")
                .authority(Role.USER)
                .loginMethod("google")
                .build();
        when(accountRepository.findByEmail("oauth@test.com")).thenReturn(Optional.of(existingAccount));

        Account savedAccount = customOAuth2AccountService.saveOrUpdate(attributes);

        assertThat(savedAccount.getName()).isEqualTo("oauth-account");
        verify(accountNicknameGenerator, never()).generateAvailableNickname("existing-oauth", "oauth@test.com");
        verify(accountRepository, never()).save(existingAccount);
        verify(accountService, never()).save(existingAccount);
    }

    @Test
    void saveOrUpdate_generatesUniqueNicknameForNewOAuthAccount() {
        OAuthAttributes attributes = OAuthAttributes.builder()
                .name("social account")
                .email("oauth-new@test.com")
                .emailVerified(true)
                .loginMethod("google")
                .attributes(Map.of())
                .nameAttributeKey("sub")
                .build();
        when(accountRepository.findByEmail("oauth-new@test.com")).thenReturn(Optional.empty());
        when(accountNicknameGenerator.generateAvailableNickname("social account", "oauth-new@test.com"))
                .thenReturn("socialaccount");
        when(accountService.save(org.mockito.ArgumentMatchers.any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Account savedAccount = customOAuth2AccountService.saveOrUpdate(attributes);

        assertThat(savedAccount.getName()).isEqualTo("socialaccount");
        verify(accountService).save(org.mockito.ArgumentMatchers.any(Account.class));
    }

    @Test
    void saveOrUpdate_retriesWhenGeneratedNicknameIsTakenDuringSave() {
        OAuthAttributes attributes = OAuthAttributes.builder()
                .name("social account")
                .email("oauth-race@test.com")
                .emailVerified(true)
                .loginMethod("google")
                .attributes(Map.of())
                .nameAttributeKey("sub")
                .build();
        when(accountRepository.findByEmail("oauth-race@test.com")).thenReturn(Optional.empty());
        when(accountNicknameGenerator.generateAvailableNickname("social account", "oauth-race@test.com"))
                .thenReturn("socialaccount", "socialaccount-2");
        when(accountService.save(org.mockito.ArgumentMatchers.any(Account.class)))
                .thenThrow(new AppException(AccountErrorCode.ACCOUNT_NAME_DUPLICATED))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Account savedAccount = customOAuth2AccountService.saveOrUpdate(attributes);

        assertThat(savedAccount.getName()).isEqualTo("socialaccount-2");
    }
}

