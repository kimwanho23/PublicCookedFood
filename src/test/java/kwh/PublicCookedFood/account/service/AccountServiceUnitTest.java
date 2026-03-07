package kwh.PublicCookedFood.account.service;

import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceUnitTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void findByPhoneNumberAndBirthDate_normalizesPhoneNumber() {
        Account account = createAccount(10L, "recover@test.com");
        LocalDate birthDate = LocalDate.of(1990, 1, 2);
        when(accountRepository.findByPhoneNumberAndBirthDate("01012345678", birthDate))
                .thenReturn(Optional.of(account));

        Optional<Account> result = accountService.findByPhoneNumberAndBirthDate("010-1234-5678", birthDate);

        assertThat(result).contains(account);
        verify(accountRepository).findByPhoneNumberAndBirthDate("01012345678", birthDate);
    }

    @Test
    void findByPhoneNumberAndBirthDate_returnsEmptyWhenInputIsBlank() {
        Optional<Account> result = accountService.findByPhoneNumberAndBirthDate(" ", null);

        assertThat(result).isEmpty();
        verifyNoInteractions(accountRepository);
    }

    @Test
    void findByEmailAndPhoneNumberAndBirthDate_normalizesAllInputs() {
        Account account = createAccount(22L, "recover2@test.com");
        LocalDate birthDate = LocalDate.of(1992, 2, 3);
        when(accountRepository.findByEmailAndPhoneNumberAndBirthDate("recover2@test.com", "01022223333", birthDate))
                .thenReturn(Optional.of(account));

        Optional<Account> result = accountService.findByEmailAndPhoneNumberAndBirthDate(
                " recover2@test.com ",
                "010-2222-3333",
                birthDate);

        assertThat(result).contains(account);
        verify(accountRepository).findByEmailAndPhoneNumberAndBirthDate("recover2@test.com", "01022223333", birthDate);
    }

    private Account createAccount(Long id, String email) {
        return Account.builder()
                .id(id)
                .email(email)
                .name("테스터")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }
}
