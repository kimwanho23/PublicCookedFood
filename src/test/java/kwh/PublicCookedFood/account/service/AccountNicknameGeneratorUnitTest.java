package kwh.PublicCookedFood.account.service;

import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountNicknameGeneratorUnitTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountNicknameGenerator accountNicknameGenerator;

    @Test
    void generateAvailableNickname_returnsPreferredNameWhenAvailable() {
        when(accountRepository.findByName("hong")).thenReturn(Optional.empty());

        String result = accountNicknameGenerator.generateAvailableNickname("hong", "hong@test.com");

        assertThat(result).isEqualTo("hong");
    }

    @Test
    void generateAvailableNickname_appendsSequenceWhenPreferredNameAlreadyExists() {
        when(accountRepository.findByName("tester")).thenReturn(Optional.of(existingAccount("tester")));
        when(accountRepository.findByName("tester-2")).thenReturn(Optional.empty());

        String result = accountNicknameGenerator.generateAvailableNickname("tester", "tester@test.com");

        assertThat(result).isEqualTo("tester-2");
    }

    @Test
    void generateAvailableNickname_fallsBackToEmailLocalPartWhenProviderNameIsNotMentionSafe() {
        when(accountRepository.findByName("socialaccount")).thenReturn(Optional.empty());

        String result = accountNicknameGenerator.generateAvailableNickname("!!!", "social.account@test.com");

        assertThat(result).isEqualTo("socialaccount");
    }

    private Account existingAccount(String name) {
        return Account.builder()
                .id(1L)
                .email(name + "@test.com")
                .name(name)
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }
}

